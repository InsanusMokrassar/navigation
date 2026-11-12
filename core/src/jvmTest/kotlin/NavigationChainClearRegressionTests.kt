import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationChainClearRegressionTests {
    @Test
    fun clearingAnEmptyChainReturns() = runInChild("empty")

    @Test
    fun ordinaryPopAllowsTheNavigationQueueToProgress() = runInChild("pop")

    @Test
    fun clearReturnsWithoutWaitingForItsOwnNavigationQueue() = runInChild("clear")

    @Test
    fun removeSubChainReturnsWithoutWaitingForItsOwnNavigationQueue() = runInChild("removeSubChain")

    @Test
    fun dropItselfReturnsWithoutWaitingForItsOwnNavigationQueue() = runInChild("dropItself")

    private fun runInChild(operation: String) {
        // clear() can spin without suspending, so coroutine cancellation cannot bound this test.
        val directory = Files.createTempDirectory("navigation-clear-test-").toFile()
        val entered = File(directory, "entered")
        val output = File(directory, "output")
        var process: Process? = null
        try {
            val java = File(System.getProperty("java.home"), "bin/java").absolutePath
            process = ProcessBuilder(
                java, "-Xmx128m", "-cp", childClasspath(),
                NavigationChainClearChild::class.java.name, operation, entered.absolutePath
            ).redirectErrorStream(true).redirectOutput(output).start()

            // Startup and node creation have their own deadline. A missing marker is a harness
            // failure, never evidence that the navigation operation hung.
            val startupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (!entered.isFile && process.isAlive && System.nanoTime() < startupDeadline) {
                Thread.sleep(10)
            }
            val reachedOperation = entered.isFile
            val finished = reachedOperation && process.waitFor(2, TimeUnit.SECONDS)
            if (process.isAlive) {
                process.destroyForcibly()
                assertTrue(process.waitFor(5, TimeUnit.SECONDS), "The isolated navigation JVM must be reaped")
            }
            val diagnostics = output.readText().takeLast(4_000)

            assertTrue(reachedOperation, "Child setup failed before $operation:\n$diagnostics")
            assertTrue(
                finished,
                "$operation must return so queued navigation commands can run; the child exceeded 2 seconds:\n$diagnostics"
            )
            assertEquals(0, process.exitValue(), "$operation must complete successfully:\n$diagnostics")
            assertTrue("COMPLETED:$operation" in diagnostics, "The child must verify the completed operation")
        } finally {
            process?.let {
                if (it.isAlive) {
                    it.destroyForcibly()
                    check(it.waitFor(5, TimeUnit.SECONDS)) { "Unable to terminate the isolated navigation JVM" }
                }
            }
            directory.deleteRecursively()
        }
    }

    private fun childClasspath(): String {
        val entries = linkedSetOf<String>()
        entries += System.getProperty("java.class.path").split(File.pathSeparator)
        // Gradle's worker classpath can contain only gradle-worker.jar; its test classloader
        // carries the actual test classes and runtime dependencies.
        var loader: ClassLoader? = javaClass.classLoader
        while (loader != null) {
            if (loader is URLClassLoader) {
                entries += loader.urLs.filter { it.protocol == "file" }.map { File(it.toURI()).absolutePath }
            }
            loader = loader.parent
        }
        return entries.joinToString(File.pathSeparator)
    }
}

/** Runs only in a disposable JVM because the regressed clear loop does not cooperate with cancellation. */
@OptIn(ExperimentalCoroutinesApi::class)
object NavigationChainClearChild {
    @JvmStatic
    fun main(args: Array<String>) {
        val operation = args[0]
        val entered = File(args[1])
        KSLog.default = KSLog("clear-regression", LogLevel.ERROR)
        val scheduler = TestCoroutineScheduler()
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(scheduler) +
                CoroutineExceptionHandler { _, failure -> errors += failure }
        )
        val root = NavigationChain(null, NavigationNodeFactory<String> { chain, label ->
            NavigationNode.Empty(chain, label)
        })
        root.start(scope)
        var fatalFailure = false
        try {
            if (operation == "empty") {
                scheduler.runCurrent()
                check(root.stackFlow.value.isEmpty())
                entered.writeText(operation)
                root.clear()
                scheduler.runCurrent()
                check(root.stackFlow.value.isEmpty())
            } else {
                val parent = checkNotNull(root.push("parent"))
                scheduler.runCurrent()
                check(parent.state == NavigationNodeState.RESUMED)
                check(root.stackFlow.value == listOf(parent))
                val (node, chain) = when (operation) {
                    "removeSubChain", "dropItself" -> checkNotNull(parent.createSubChain("child"))
                    else -> parent to root
                }
                scheduler.runCurrent()
                check(node.state == NavigationNodeState.RESUMED)
                check(chain.stackFlow.value == listOf(node))

                // The scheduler has no concurrent worker. These synchronous APIs must return
                // before any queued drop can run, as on a single-thread UI dispatcher.
                entered.writeText(operation)
                when (operation) {
                    "pop" -> check(chain.pop() === node)
                    "clear" -> chain.clear()
                    "removeSubChain" -> check(parent.removeSubChain(chain))
                    "dropItself" -> check(chain.dropItself())
                    else -> error("Unknown operation: $operation")
                }
                scheduler.runCurrent()
                check(chain.stackFlow.value.isEmpty()) { "$operation must remove the queued node" }
                check(node.state == NavigationNodeState.NEW) { "$operation must finish node destruction" }
                if (chain !== root) check(chain !in parent.subchains)
            }
            check(errors.isEmpty()) { "Unexpected navigation errors: $errors" }
            println("COMPLETED:$operation")
        } catch (failure: VirtualMachineError) {
            fatalFailure = true
            throw failure
        } finally {
            // A regressed clear can exhaust this disposable JVM's capped heap. Cancellation
            // allocates too, so preserve the original failure instead of masking it in cleanup.
            if (!fatalFailure) {
                scope.cancel()
                scheduler.runCurrent()
            }
        }
    }
}
