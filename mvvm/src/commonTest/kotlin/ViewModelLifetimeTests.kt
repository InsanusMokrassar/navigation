import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.invoke
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelLifetimeTests {
    private lateinit var previousLogger: KSLog

    @BeforeTest
    fun useTestLogger() {
        previousLogger = KSLog.default
        KSLog.default = KSLog { level, tag, message, throwable ->
            println("$level/$tag: $message")
            throwable?.let { println(it) }
        }
    }

    @AfterTest
    fun restoreLogger() {
        KSLog.default = previousLogger
    }

    private class Node(override val chain: NavigationChain<String>, config: String) : NavigationNode<String, String>() {
        override val configState = MutableStateFlow(config)
        var beforePause: suspend () -> Unit = {}
        override suspend fun onBeforePause() = beforePause()
    }

    private class Model(node: NavigationNode<out String, String>) : ViewModel<String>(node) {
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val work = scope.launch {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                stopped.complete(Unit)
            }
        }

        suspend fun awaitStarted() = withContext(Dispatchers.Default) {
            withTimeout(5_000) { started.await() }
        }

        suspend fun assertStopped() = withContext(Dispatchers.Default) {
            withTimeout(5_000) { work.join() }
            assertFalse(scope.isActive)
            assertTrue(stopped.isCompleted)
        }
    }

    private fun chain() = NavigationChain(null, NavigationNodeFactory<String> { chain, config -> Node(chain, config) })

    @Test
    fun pausingAndRestartingWorkersKeepsTheLiveModelAndItsWork() = runTest {
        val chain = chain()
        val node = assertNotNull(chain.push("root"))
        val model = Model(node)
        var job = chain.start(backgroundScope)
        try {
            runCurrent()
            model.awaitStarted()
            assertEquals(NavigationNodeState.RESUMED, node.state)

            node.changeState(NavigationNodeState.STARTED)
            assertTrue(model.scope.isActive && model.work.isActive)
            job.cancelAndJoin()
            assertTrue(node.lifetime.isActive && model.scope.isActive && model.work.isActive)

            job = chain.start(backgroundScope)
            runCurrent()

            assertTrue(job.isActive)
            assertEquals(NavigationNodeState.RESUMED, node.state)
            assertTrue(model.scope.isActive && model.work.isActive)
            assertFalse(model.stopped.isCompleted)
        } finally {
            chain.dispose()
            job.cancelAndJoin()
        }
        model.assertStopped()
    }

    @Test
    fun disposingAnUnstartedHierarchyCancelsPendingDescendantsAndLateModels() = runTest {
        val chain = chain()
        val node = assertNotNull(chain.push("pending root"))
        val (child, childChain) = assertNotNull(node.createSubChain("pending child"))
        val models = listOf(Model(node), Model(child))
        try {
            models.forEach { it.awaitStarted() }
            assertTrue(chain.stackFlow.value.isEmpty() && childChain.stackFlow.value.isEmpty())

            chain.dispose()
            chain.dispose()

            models.forEach { it.assertStopped() }
            assertFailsWith<IllegalStateException> { chain.start(backgroundScope) }
            assertFailsWith<IllegalStateException> { childChain.start(backgroundScope) }
            assertNull(chain.push("after disposal"))
            assertNull(childChain.push("after disposal"))
            val lateModel = Model(child)
            assertFalse(lateModel.scope.isActive)
            assertTrue(lateModel.work.isCancelled)
        } finally {
            chain.dispose()
        }
    }

    @Test
    fun terminalDisposalCancelsWorkersWithoutWaitingForUiCallbacks() = runTest {
        val chain = chain()
        val node = assertNotNull(chain.push("root")) as Node
        val model = Model(node)
        val job = chain.start(backgroundScope)
        var callbackEntered = false
        try {
            runCurrent()
            model.awaitStarted()
            assertEquals(NavigationNodeState.RESUMED, node.state)
            node.beforePause = {
                callbackEntered = true
                awaitCancellation()
            }

            chain.dispose()

            assertTrue(job.isCancelled)
            model.assertStopped()
            assertFalse(callbackEntered)
            assertNull(chain.push("after disposal"))
        } finally {
            chain.dispose()
            job.cancelAndJoin()
        }
    }

    @Test
    fun clearingQueuedNodesCancelsModelsBeforeTheirFirstLifecycleTransition() = runTest {
        val chain = chain()
        val node = assertNotNull(chain.push("pending root"))
        val (child, _) = assertNotNull(node.createSubChain("pending child"))
        val models = listOf(Model(node), Model(child))
        models.forEach { it.awaitStarted() }
        chain.clear()
        val job = chain.start(backgroundScope)
        try {
            runCurrent()

            assertTrue(chain.stackFlow.value.isEmpty())
            assertEquals(NavigationNodeState.NEW, node.state)
            assertEquals(NavigationNodeState.NEW, child.state)
            models.forEach { it.assertStopped() }
            assertTrue(job.isActive)
        } finally {
            chain.dispose()
            job.cancelAndJoin()
        }
    }

    @Test
    fun removingTheLastChildDisposesItsChainAndPreservesTheParent() = runTest {
        val chain = chain()
        val parent = assertNotNull(chain.push("root"))
        val (child, childChain) = assertNotNull(parent.createSubChain("child"))
        val parentModel = Model(parent)
        val childModel = Model(child)
        val job = chain.start(backgroundScope)
        try {
            runCurrent()
            parentModel.awaitStarted()
            childModel.awaitStarted()
            childChain.drop(child)
            runCurrent()

            childModel.assertStopped()
            assertFalse(childChain in parent.subchains)
            assertNull(childChain.push("after removal"))
            assertFailsWith<IllegalStateException> { childChain.start(backgroundScope) }
            assertTrue(job.isActive && parentModel.scope.isActive && parentModel.work.isActive)
        } finally {
            chain.dispose()
            job.cancelAndJoin()
        }
        parentModel.assertStopped()
    }

    @Test
    fun ordinaryNodeRemovalStillCancelsItsModel() = runTest {
        val chain = chain()
        val node = assertNotNull(chain.push("root"))
        val model = Model(node)
        val job = chain.start(backgroundScope)
        try {
            runCurrent()
            model.awaitStarted()
            chain.drop(node)
            runCurrent()

            assertEquals(NavigationNodeState.NEW, node.state)
            model.assertStopped()
            assertTrue(job.isActive)
        } finally {
            chain.dispose()
            job.cancelAndJoin()
        }
    }
}
