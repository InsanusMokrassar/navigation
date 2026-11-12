import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.invoke
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.extensions.dropAndAwaitDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationChainLifecycleRegressionTests : NavigationTest() {
    private class RecordingNode(
        override val chain: NavigationChain<String>,
        label: String,
    ) : NavigationNode<String, String>() {
        override val configState = MutableStateFlow(label)
        val teardownCallbacks = mutableListOf<String>()
        var beforePause: suspend () -> Unit = {}
        var beforeDestroy: suspend () -> Unit = {}

        override suspend fun onBeforePause() {
            teardownCallbacks += "beforePause"
            beforePause()
        }

        override fun onPause() { teardownCallbacks += "pause" }
        override suspend fun onBeforeStop() { teardownCallbacks += "beforeStop" }
        override fun onStop() { teardownCallbacks += "stop" }

        override suspend fun onBeforeDestroy() {
            teardownCallbacks += "beforeDestroy"
            beforeDestroy()
        }

        override fun onDestroy() { teardownCallbacks += "destroy" }
    }

    private class DestroyGate {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var completed = false
        var cancelled = false

        suspend fun awaitRelease() {
            entered.complete(Unit)
            try {
                release.await()
                completed = true
            } catch (failure: CancellationException) {
                cancelled = true
                throw failure
            }
        }
    }

    private class Fixture(
        private val scheduler: TestCoroutineScheduler,
        val reportedErrors: MutableList<Throwable>,
    ) {
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(scheduler) +
                CoroutineExceptionHandler { _, failure -> reportedErrors += failure }
        )
        val root = NavigationChain(null, NavigationNodeFactory<String> { chain, label ->
            RecordingNode(chain, label)
        })
        val rootJob = root.start(scope)

        fun push(label: String, chain: NavigationChain<String> = root): RecordingNode {
            val node = assertNotNull(chain.push(label)) as RecordingNode
            scheduler.runCurrent()
            assertTrue(node in chain.stackFlow.value, "Setup must add $label to its chain")
            assertEquals(NavigationNodeState.RESUMED, node.state, "Setup must resume $label")
            return node
        }

        fun child(parent: RecordingNode, label: String): Pair<RecordingNode, NavigationChain<String>> {
            val (node, chain) = assertNotNull(parent.createSubChain(label))
            scheduler.runCurrent()
            assertTrue(chain in parent.subchains, "Setup must attach the child chain")
            assertEquals(NavigationNodeState.RESUMED, node.state, "Setup must resume $label")
            return (node as RecordingNode) to chain
        }
    }

    private fun runNavigationTest(block: suspend TestScope.(Fixture) -> Unit) = runTest(timeout = 5.seconds) {
        val previousLogger = KSLog.default
        val reportedErrors = mutableListOf<Throwable>()
        // Accept error reporting through either the logger or the coroutine exception handler.
        KSLog.default = KSLog { _, _, _, failure -> failure?.let(reportedErrors::add) }
        val fixture = Fixture(testScheduler, reportedErrors)
        try {
            block(fixture)
        } finally {
            fixture.scope.cancel()
            runCurrent()
            KSLog.default = previousLogger
        }
    }

    @Test
    fun successfulDropAllowsTheNextPushToRun() = runNavigationTest { fixture ->
        val first = fixture.push("first")
        val dropping = fixture.scope.async { fixture.root.dropAndAwaitDestroy(first) }
        runCurrent()
        val next = fixture.push("next")

        assertTrue(dropping.isCompleted && !dropping.isCancelled)
        assertEquals(NavigationNodeState.NEW, first.state)
        assertEquals(listOf(next), fixture.root.stackFlow.value)
        assertTrue(fixture.rootJob.isActive)
        assertTrue(fixture.reportedErrors.isEmpty())
    }

    @Test
    fun failingDropCallbackReportsTheErrorAndAllowsTheNextPushToRun() = runNavigationTest { fixture ->
        val first = fixture.push("first")
        val failure = IllegalStateException("onBeforePause failed for first")
        first.beforePause = {
            // Fail only this drop; a later state actualization must be able to succeed.
            first.beforePause = {}
            throw failure
        }

        assertSame(first, fixture.root.drop(first))
        runCurrent()
        assertTrue("beforePause" in first.teardownCallbacks, "The failing callback must have run")
        assertTrue(
            fixture.reportedErrors.any { it === failure },
            "A failed lifecycle callback must report its original exception"
        )

        val next = assertNotNull(fixture.root.push("next"))
        runCurrent()

        assertTrue(
            next in fixture.root.stackFlow.value,
            "A lifecycle failure must not strand later commands; chain job active=${fixture.rootJob.isActive}"
        )
        assertEquals(NavigationNodeState.RESUMED, next.state)
        assertTrue(fixture.rootJob.isActive, "A usable chain must retain its running job")
    }

    @Test
    fun rootDropWaitsForASuspendingDestroyCallback() = runNavigationTest { fixture ->
        val node = fixture.push("root child")
        val gate = DestroyGate()
        node.beforeDestroy = gate::awaitRelease
        val dropping = fixture.scope.async { fixture.root.dropAndAwaitDestroy(node) }
        runCurrent()

        assertTrue(gate.entered.isCompleted)
        assertEquals(NavigationNodeState.CREATED, node.state)
        assertFalse(dropping.isCompleted, "Destroy must remain pending while the callback is suspended")
        assertFalse(gate.cancelled)

        gate.release.complete(Unit)
        runCurrent()

        assertTrue(gate.completed)
        assertEquals(NavigationNodeState.NEW, node.state)
        assertTrue(dropping.isCompleted && !dropping.isCancelled)
        assertEquals(1, node.teardownCallbacks.count { it == "destroy" })
    }

    @Test
    fun nonLastChildDropWaitsForASuspendingDestroyCallback() = runNavigationTest { fixture ->
        val parent = fixture.push("parent")
        val (remaining, subchain) = fixture.child(parent, "remaining")
        val removed = fixture.push("removed", subchain)
        val gate = DestroyGate()
        removed.beforeDestroy = gate::awaitRelease
        val dropping = fixture.scope.async { subchain.dropAndAwaitDestroy(removed) }
        runCurrent()

        assertTrue(gate.entered.isCompleted)
        assertFalse(dropping.isCompleted)
        assertFalse(gate.cancelled)

        gate.release.complete(Unit)
        runCurrent()

        assertTrue(gate.completed)
        assertEquals(NavigationNodeState.NEW, removed.state)
        assertTrue(dropping.isCompleted && !dropping.isCancelled)
        assertEquals(listOf(remaining), subchain.stackFlow.value)
        assertTrue(subchain in parent.subchains)
        assertEquals(NavigationNodeState.RESUMED, remaining.state)
    }

    @Test
    fun lastChildDropFinishesDestructionAndReleasesItsWaiter() = runNavigationTest { fixture ->
        val parent = fixture.push("parent")
        val (child, subchain) = fixture.child(parent, "last child")
        val gate = DestroyGate()
        child.beforeDestroy = gate::awaitRelease
        val dropping = fixture.scope.async { subchain.dropAndAwaitDestroy(child) }
        runCurrent()

        assertTrue(gate.entered.isCompleted, "The last child's destroy callback must be entered")
        assertFalse(dropping.isCompleted, "The waiter must not return before destruction completes")

        gate.release.complete(Unit)
        runCurrent()

        assertTrue(
            gate.completed && !gate.cancelled,
            "Removing the last child must not cancel its destroy callback; state=${child.state}, " +
                "attached=${subchain in parent.subchains}, waiter active=${dropping.isActive}"
        )
        assertEquals(NavigationNodeState.NEW, child.state)
        assertTrue(dropping.isCompleted && !dropping.isCancelled, "dropAndAwaitDestroy must return")
        assertTrue(subchain.stackFlow.value.isEmpty())
        assertFalse(subchain in parent.subchains)
        assertEquals(1, child.teardownCallbacks.count { it == "destroy" })
    }

    @Test
    fun droppingAParentDestroysAllChildrenAndGrandchildren() = runNavigationTest { fixture ->
        val parent = fixture.push("parent")
        val (firstChild, _) = fixture.child(parent, "first child")
        val (grandchild, _) = fixture.child(firstChild, "grandchild")
        val (secondChild, _) = fixture.child(parent, "second child")
        val nodes = listOf(parent, firstChild, grandchild, secondChild)

        val dropping = fixture.scope.async { fixture.root.dropAndAwaitDestroy(parent) }
        runCurrent()

        assertEquals(
            nodes.map { it.config to NavigationNodeState.NEW },
            nodes.map { it.config to it.state },
            "Dropping a parent must destroy every node in its subtree before cancelling their jobs"
        )
        val expectedCallbacks = listOf("beforePause", "pause", "beforeStop", "stop", "beforeDestroy", "destroy")
        nodes.forEach { node ->
            assertEquals(expectedCallbacks, node.teardownCallbacks, "Teardown callbacks for ${node.config}")
        }
        assertTrue(dropping.isCompleted && !dropping.isCancelled)
    }

    @Test
    fun droppingAParentAllowsASuspendingDescendantDestroyToFinish() = runNavigationTest { fixture ->
        val parent = fixture.push("parent")
        val (child, _) = fixture.child(parent, "child")
        val (grandchild, _) = fixture.child(child, "grandchild")
        val gate = DestroyGate()
        grandchild.beforeDestroy = gate::awaitRelease
        fixture.root.drop(parent)
        runCurrent()

        assertTrue(gate.entered.isCompleted, "Dropping a parent must enter its descendant's destroy callback")
        gate.release.complete(Unit)
        runCurrent()

        assertTrue(gate.completed && !gate.cancelled, "Subtree cancellation must wait for descendant destruction")
        listOf(parent, child, grandchild).forEach { node ->
            assertEquals(NavigationNodeState.NEW, node.state, "Final state of ${node.config}")
            assertEquals(1, node.teardownCallbacks.count { it == "destroy" }, "Destruction of ${node.config}")
        }
    }

    @Test
    fun reentrantStateChangeIsRejectedOrCompletesWithoutStrandingCommands() = runNavigationTest { fixture ->
        val first = fixture.push("first")
        var rejection: Throwable? = null
        first.beforePause = {
            first.beforePause = {}
            try {
                first.changeState(NavigationNodeState.CREATED)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                // Both explicit rejection and deferred completion are valid reentrancy policies.
                rejection = failure
            }
        }

        val dropping = fixture.scope.async { fixture.root.dropAndAwaitDestroy(first) }
        runCurrent()
        assertTrue("beforePause" in first.teardownCallbacks, "The reentrant callback must have run")
        val next = assertNotNull(fixture.root.push("next"))
        runCurrent()

        assertTrue(
            dropping.isCompleted && !dropping.isCancelled,
            "A reentrant changeState from onBeforePause must finish or reject explicitly, not wait on its own mutex"
        )
        assertTrue(next in fixture.root.stackFlow.value, "Reentrancy must not stall the next queued push")
        assertEquals(NavigationNodeState.RESUMED, next.state)
        rejection?.let {
            assertFalse(it.message.isNullOrBlank(), "Reentrancy rejection must explain the invalid call")
        }
    }
}
