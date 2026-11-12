package dev.inmo.navigation.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshots.Snapshot
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class ComposePauseAnimationTests {
    private class TestNode(
        config: String,
        chain: NavigationChain<String>,
        private val animatePause: Boolean,
    ) : ComposeNode<String, String>(config, chain) {
        var composed = false
            private set
        var animationCompleted = false
            private set

        override suspend fun useBeforePauseWait(): Boolean = animatePause

        @Composable
        override fun onDraw() {
            DisposableEffect(Unit) {
                composed = true
                onDispose { composed = false }
            }
            val pauseJob = beforePauseWaitJobState.collectAsState().value
            if (pauseJob != null) {
                LaunchedEffect(pauseJob) {
                    delay(100)
                    animationCompleted = true
                    pauseJob.complete()
                }
            }
        }
    }

    private class NoOpApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }

    private class NavigationComposition(
        private val testScope: TestScope,
        animateFirstNode: Boolean,
    ) {
        private val frameClock = BroadcastFrameClock()
        private val scope = CoroutineScope(
            testScope.backgroundScope.coroutineContext +
                SupervisorJob(testScope.backgroundScope.coroutineContext[Job]) + frameClock
        )
        private val recomposer = Recomposer(scope.coroutineContext)
        private val composition = Composition(NoOpApplier(), recomposer)
        val chain = NavigationChain<String>(null, NavigationNodeFactory { chain, config ->
            TestNode(config, chain, animateFirstNode && config == "first")
        })

        init {
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
            chain.start(scope)
            composition.setContent {
                doWithChainInLocalProvider(chain) {
                    DrawStackNodes<String>()
                }
            }
        }

        fun push(config: String): TestNode = chain.push(config) as TestNode

        fun awaitFrames(message: String, condition: () -> Boolean) {
            // Drive a real composition with virtual time; failures cannot hang the test runner.
            repeat(125) {
                testScope.runCurrent()
                Snapshot.sendApplyNotifications()
                testScope.runCurrent()
                frameClock.sendFrame(testScope.testScheduler.currentTime * 1_000_000)
                testScope.runCurrent()
                if (condition()) return
                testScope.advanceTimeBy(16)
            }
            fail("$message; stack=${chain.stackFlow.value.map { it.config to it.state }}")
        }

        fun close() {
            composition.dispose()
            recomposer.cancel()
            scope.cancel()
            testScope.runCurrent()
        }
    }

    private inline fun TestScope.withNavigation(
        animateFirstNode: Boolean,
        block: NavigationComposition.() -> Unit,
    ) {
        val navigation = NavigationComposition(this, animateFirstNode)
        try {
            navigation.block()
        } finally {
            navigation.close()
        }
    }

    @Test
    fun pushingNextNodeKeepsPauseAnimationAliveUntilNavigationCompletes() = runTest {
        withNavigation(animateFirstNode = true) {
            val first = push("first")
            awaitFrames("The initial node must be resumed and drawn") {
                first.state == NavigationNodeState.RESUMED && first.composed
            }

            val second = push("second")
            awaitFrames("The outgoing animation must complete and allow the next node to resume") {
                first.animationCompleted && second.state == NavigationNodeState.RESUMED && second.composed
            }
            assertEquals(NavigationNodeState.STARTED, first.state)

            val third = push("third")
            awaitFrames("Navigation must continue processing pushes after the animation") {
                third.state == NavigationNodeState.RESUMED && third.composed
            }
            assertEquals(listOf("first", "second", "third"), chain.stackFlow.value.map { it.config })
        }
    }

    @Test
    fun pushingWithoutPauseAnimationResumesTheNextNode() = runTest {
        withNavigation(animateFirstNode = false) {
            val first = push("first")
            awaitFrames("The initial node must be resumed and drawn") {
                first.state == NavigationNodeState.RESUMED && first.composed
            }

            val second = push("second")
            awaitFrames("A push without an exit animation must resume the next node") {
                second.state == NavigationNodeState.RESUMED && second.composed
            }
            assertEquals(NavigationNodeState.STARTED, first.state)
        }
    }

    @Test
    fun poppingAnimatedNodeLetsItsCompositionCompleteThePause() = runTest {
        withNavigation(animateFirstNode = true) {
            val first = push("first")
            awaitFrames("The initial node must be resumed and drawn") {
                first.state == NavigationNodeState.RESUMED && first.composed
            }

            chain.pop()
            awaitFrames("A pop must finish the animation before destroying the node") {
                first.animationCompleted && first.state == NavigationNodeState.NEW
            }
            assertTrue(chain.stackFlow.value.isEmpty())
        }
    }
}
