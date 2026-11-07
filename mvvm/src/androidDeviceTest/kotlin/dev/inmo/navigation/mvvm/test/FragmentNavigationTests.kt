package dev.inmo.navigation.mvvm.test

import androidx.test.filters.LargeTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.dropAndAwaitAbsence
import dev.inmo.navigation.core.extensions.dropAndAwaitDestroy
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.mvvm.test.NavigationFixture.Companion.ROOT
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
@LargeTest
class FragmentNavigationTests(private val animationsEnabled: Boolean) {
    @Test
    fun nestedUnstorablePushesAndPopsLeaveSiblingChainIntact() {
        fixture().use { f ->
            val a = f.top()
            val left = f.child(a, "left", "Left")
            val right = f.child(a, "right", "Right")
            val detail = f.child(left, "detail", "Detail")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
                "detail" to listOf("Detail"),
            )
            val siblingModel = f.model(right)

            val first = f.push(detail.chain, "Transient1", storable = false)
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
                "detail" to listOf("Detail", "Transient1"),
            )
            val second = f.push(detail.chain, "Transient2", storable = false)
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
                "detail" to listOf("Detail", "Transient1", "Transient2"),
            )

            assertSame(second, f.pop(detail.chain))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
                "detail" to listOf("Detail", "Transient1"),
            )
            f.awaitDestroyed(second)

            assertSame(first, f.pop(detail.chain))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
                "detail" to listOf("Detail"),
            )
            f.awaitDestroyed(first)
            assertSame(siblingModel, f.model(right))
            assertTrue(siblingModel.scope.isActive)
            if (animationsEnabled) assertTrue(f.main { f.startedTransitionCount > 0 })
        }
    }

    @Test
    fun burstPushesAndPopDuringTransitionReachTheFinalStack() {
        fixture().use { f ->
            f.expect(ROOT to listOf("A"))
            val (b, c) = f.main {
                f.push(f.root, "B") to f.push(f.root, "C")
            }

            // Reverse the accepted stack while its effect is running, or immediately without effects.
            f.await("the burst stack is accepted${if (animationsEnabled) " with a running framework transition" else ""}") {
                if (f.root.labels() == listOf("A", "B", "C") &&
                    (!animationsEnabled || f.activeTransitionCount > 0)
                ) {
                    assertSame(c, f.pop(f.root))
                    true
                } else {
                    false
                }
            }
            f.expect(ROOT to listOf("A", "B"))
            f.awaitDestroyed(c)
            assertTrue(f.model(b).scope.isActive)

            assertSame(b, f.pop(f.root))
            f.expect(ROOT to listOf("A"))
            f.awaitDestroyed(b)
        }
    }

    @Test
    fun parentPushAndPopRestoreBothChildChains() {
        fixture().use { f ->
            val a = f.top()
            val left = f.child(a, "left", "Left")
            val right = f.child(a, "right", "Right")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left"),
                "right" to listOf("Right"),
            )
            val leftTop = f.push(left.chain, "Left2")
            val rightTop = f.push(right.chain, "Right2")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left", "Left2"),
                "right" to listOf("Right", "Right2"),
            )
            val leftModel = f.model(leftTop)
            val rightModel = f.model(rightTop)

            val b = f.push(f.root, "B")
            f.expect(
                ROOT to listOf("A", "B"),
                "left" to listOf("Left", "Left2"),
                "right" to listOf("Right", "Right2"),
            )
            assertSame(b, f.pop(f.root))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Left", "Left2"),
                "right" to listOf("Right", "Right2"),
            )
            f.awaitDestroyed(b)
            assertSame(leftModel, f.model(leftTop))
            assertSame(rightModel, f.model(rightTop))
            assertTrue(leftModel.scope.isActive)
            assertTrue(rightModel.scope.isActive)
        }
    }

    @Test
    fun activeReplacementsAndMiddleDropKeepTheStackUsable() {
        fixture().use { f ->
            val b = f.push(f.root, "B")
            f.expect(ROOT to listOf("A", "B"))
            val c = f.push(f.root, "C")
            f.expect(ROOT to listOf("A", "B", "C"))

            val d = f.replace(c, "D")
            f.expect(ROOT to listOf("A", "B", "D"))
            f.awaitDestroyed(c)
            val e = f.replace(d, "E", reversed = true)
            f.expect(ROOT to listOf("A", "B", "E"))
            f.awaitDestroyed(d)

            f.drop(b)
            f.expect(ROOT to listOf("A", "E"))
            f.awaitDestroyed(b)
            assertSame(e, f.pop(f.root))
            f.expect(ROOT to listOf("A"))
            f.awaitDestroyed(e)
        }
    }

    @Test
    fun dropAndAwaitAbsenceSupportsAllOverloadsInSequence() {
        awaitedDropsSupportAllOverloadsInSequence(awaitDestroy = false)
    }

    @Test
    fun dropAndAwaitDestroySupportsAllOverloadsInSequence() {
        awaitedDropsSupportAllOverloadsInSequence(awaitDestroy = true)
    }

    @Test
    fun dropAndAwaitAbsenceWaitsForBeforePauseGate() {
        awaitedDropWaitsForBeforePauseGate(awaitDestroy = false)
    }

    @Test
    fun dropAndAwaitDestroyWaitsForBeforePauseGate() {
        awaitedDropWaitsForBeforePauseGate(awaitDestroy = true)
    }

    @Test
    fun beforePauseGateKeepsTheOutgoingNodeAndViewModelActive() {
        fixture().use { f ->
            f.expect(ROOT to listOf("A"))
            val a = f.top()
            val model = f.model(a)
            f.main { a.useBeforePauseWait = true }
            val b = f.push(f.root, "B")

            f.await("the outgoing node is waiting before pause") {
                a.beforePauseWaitJobState.value != null
            }
            f.main {
                assertEquals(listOf("A", "B"), f.root.labels())
                assertEquals(NavigationNodeState.RESUMED, a.state)
                assertNotEquals(NavigationNodeState.RESUMED, b.state)
                assertTrue(f.hasResumedView(a))
                assertFalse(f.hasResumedView(b))
                assertTrue(model.scope.isActive)
                assertTrue(assertNotNull(a.beforePauseWaitJobState.value).complete())
            }

            f.expect(ROOT to listOf("A", "B"))
            assertSame(model, f.model(a))
            assertTrue(model.scope.isActive)
        }
    }

    @Test
    fun recreationRestoresNestedStacksWithoutAnUnstorableTop() {
        fixture().use { f ->
            populateRestorableTree(f)
            val expectedSavedLabels = setOf("A", "Left", "Left2", "Right", "Detail")
            f.await("the repository contains the complete storable hierarchy") {
                f.savedHierarchy()?.savedLabels()?.toSet() == expectedSavedLabels
            }
            val oldActivity = f.activity
            val oldRoot = f.root

            f.recreate()

            expectRestoredTree(f)
            assertNotSame(oldActivity, f.activity)
            assertNotSame(oldRoot, f.root)
            assertTrue(f.main { oldActivity.isDestroyed })
        }
    }

    @Test
    fun freshHostRestoresSerializedNestedStacksWithoutAnUnstorableTop() {
        fixture().use { f ->
            populateRestorableTree(f)
            val expectedSavedLabels = setOf("A", "Left", "Left2", "Right", "Detail")
            f.await("the repository contains the complete storable hierarchy") {
                f.savedHierarchy()?.savedLabels()?.toSet() == expectedSavedLabels
            }
            val saved = assertNotNull(f.savedHierarchy())
            assertFalse("Transient" in saved.savedLabels())
            val oldRoot = f.root
            val oldActivity = f.activity

            f.closeActivity()
            f.relaunch()

            expectRestoredTree(f)
            assertNotSame(oldRoot, f.root)
            assertNotSame(oldActivity, f.activity)
            assertEquals(expectedSavedLabels, assertNotNull(f.savedHierarchy()).savedLabels().toSet())
        }
    }

    @Test
    fun closingHostDuringTransitionCancelsNavigationAndDetachesViews() {
        fixture().use { f ->
            f.expect(ROOT to listOf("A"))
            val host = f.activity
            val navigationJob = assertNotNull(f.main { host.navigationJob })
            assertTrue(navigationJob.isActive)
            pushAndInterruptHost(
                f,
                effectDescription = "a framework transition is running before host destruction",
                runningEffect = { f.activeTransitionCount > 0 },
                interruptHost = { host.finish() },
            )

            f.closeActivity()

            f.await("the destroyed host has stopped navigation and detached its views") {
                host.isDestroyed && !navigationJob.isActive && f.allViewsDestroyed
            }
            assertFalse(navigationJob.isActive)
            assertEquals(0, f.attachedViewCount)
        }
    }

    @Test
    fun customAnimationsSupportNestedPushAndPop() {
        nestedPushAndPopSmoke(TestEffects.CUSTOM_ANIMATIONS)
    }

    @Test
    fun frameworkTransitionsSupportNestedPushAndPop() {
        nestedPushAndPopSmoke(TestEffects.FRAMEWORK)
    }

    @Test
    fun recreationWithoutLoadingFragmentRestartsNavigation() {
        fixture(withLoadingFragment = false).use { f ->
            val b = f.push(f.root, "B")
            f.expect(ROOT to listOf("A", "B"))
            f.child(b, "left", "Left")
            f.expect(ROOT to listOf("A", "B"), "left" to listOf("Left"))
            f.await("the hierarchy is saved before recreation without a Loading fragment") {
                f.savedHierarchy()?.savedLabels()?.toSet() == setOf("A", "B", "Left")
            }
            val oldHost = f.activity
            val oldJob = assertNotNull(f.main { oldHost.navigationJob })

            f.recreate()

            f.expect(ROOT to listOf("A", "B"), "left" to listOf("Left"))
            assertNotSame(oldHost, f.activity)
            assertFalse(oldJob.isActive)
            assertTrue(assertNotNull(f.main { f.activity.navigationJob }).isActive)
        }
    }

    @Test
    fun recreationBindsNodesBeforeOnCreateAndPreservesUnrelatedFragmentsAndFactory() {
        fixture(
            readNodeInOnCreate = true,
            withCustomFragmentFactory = true,
        ).use { f ->
            assertTrue("A" in f.createdNodeLabels)
            f.push(f.root, "B")
            f.expect(ROOT to listOf("A", "B"))
            val unrelated = f.addUnrelatedFragment() as UnrelatedTestFragment
            f.await("the unrelated fragment is added") { f.unrelatedFragment === unrelated && unrelated.isAdded }
            f.main { unrelated.savedMessage = "preserved across recreation" }
            f.expect(ROOT to listOf("A", "B"))
            f.await("the hierarchy is saved before recreation with a custom factory") {
                f.savedHierarchy()?.savedLabels()?.toSet() == setOf("A", "B")
            }
            val factory = assertNotNull(f.customFragmentFactory)
            val previousInstantiations = f.unrelatedInstantiationCount

            f.recreate()

            f.expect(ROOT to listOf("A", "B"))
            assertTrue("B" in f.createdNodeLabels)
            val restoredUnrelated = assertNotNull(f.unrelatedFragment) as UnrelatedTestFragment
            assertNotSame(unrelated, restoredUnrelated)
            assertEquals("preserved across recreation", f.main { restoredUnrelated.savedMessage })
            assertEquals(previousInstantiations + 1, f.unrelatedInstantiationCount)
            assertSame(factory, f.main { f.activity.supportFragmentManager.fragmentFactory })
        }
    }

    @Test
    fun closingHostDuringLegacyAnimationDestroysItsViews() {
        fixture(TestEffects.CUSTOM_ANIMATIONS).use { f ->
            val a = f.top()
            val host = f.activity
            val job = assertNotNull(f.main { host.navigationJob })
            pushAndInterruptHost(
                f,
                effectDescription = "the outgoing legacy animation is running before host destruction",
                runningEffect = { f.hasRunningAnimation(a) },
                interruptHost = { host.finish() },
            )

            f.closeActivity()

            f.await("the destroyed host has completed legacy effects and released its views") {
                host.isDestroyed && !job.isActive && f.allViewsDestroyed
            }
            if (animationsEnabled) assertTrue(f.customAnimationRequestCount > 0)
        }
    }

    @Test
    fun stoppingAndResumingDuringLegacyAnimationKeepsNavigationUsable() {
        fixture(TestEffects.CUSTOM_ANIMATIONS).use { f ->
            val root = f.root
            val a = f.top()
            val host = f.activity
            val job = assertNotNull(f.main { host.navigationJob })
            val b = pushAndInterruptHost(
                f,
                effectDescription = "a running legacy animation is interrupted by backgrounding the host",
                runningEffect = { f.hasRunningAnimation(a) },
                interruptHost = { f.backgroundHost() },
            )
            f.await("the backgrounded host reaches CREATED") {
                host.lifecycle.currentState == Lifecycle.State.CREATED
            }

            assertEquals(animationsEnabled, f.pausedWithRunningAnimation, "The host must stop with the configured effects")
            assertEquals(Lifecycle.State.CREATED, f.main { host.lifecycle.currentState })
            assertTrue(job.isActive)
            f.returnToHost()
            f.await("the navigation host resumes after its covering activity finishes") {
                host.lifecycle.currentState == Lifecycle.State.RESUMED
            }
            f.expect(ROOT to listOf("A", "B"))
            assertSame(root, f.root)
            assertSame(b, f.pop(root))
            f.expect(ROOT to listOf("A"))
            f.awaitDestroyed(b)
        }
    }

    @Test
    fun ancestorPushAfterChildTransitionCaptureCompletesPendingFrameworkEffects() {
        interruptNestedTransitionWithAncestor(TestEffects.FRAMEWORK, beforePreDraw = true)
    }

    @Test
    fun ancestorPushDuringChildTransitionCompletesRunningFrameworkEffects() {
        interruptNestedTransitionWithAncestor(TestEffects.FRAMEWORK, beforePreDraw = false)
    }

    @Test
    fun ancestorPushAfterChildTransitionCaptureHandlesConfiguredAnimationResources() {
        interruptNestedTransitionWithAncestor(TestEffects.FRAMEWORK_WITH_CUSTOM_ANIMATIONS, beforePreDraw = true)
    }

    @Test
    fun ancestorPushDuringChildTransitionHandlesConfiguredAnimationResources() {
        interruptNestedTransitionWithAncestor(TestEffects.FRAMEWORK_WITH_CUSTOM_ANIMATIONS, beforePreDraw = false)
    }

    @Test
    fun ancestorPushCompletesWithAPostponedChildEntry() {
        fixture().use { f ->
            val left = f.child(f.top(), "left", "Left")
            f.expect(ROOT to listOf("A"), "left" to listOf("Left"))
            f.postponeNextChildEntry()
            val postponed = f.push(left.chain, "Postponed")
            f.await("the postponed child has an attached view but has not resumed") {
                f.hasUnresumedView(postponed)
            }
            val model = f.model(postponed)

            val b = f.push(f.root, "B")

            // The test never releases the postponed entry; ancestor navigation must settle it.
            f.expect(ROOT to listOf("A", "B"), "left" to listOf("Left", "Postponed"))
            assertSame(b, f.pop(f.root))
            f.expect(ROOT to listOf("A"), "left" to listOf("Left", "Postponed"))
            f.awaitDestroyed(b)
            assertSame(model, f.model(postponed))
            assertTrue(model.scope.isActive)
            assertSame(postponed, f.pop(left.chain))
            f.expect(ROOT to listOf("A"), "left" to listOf("Left"))
            f.awaitDestroyed(postponed)
        }
    }

    @Test
    fun closingHostWhileAncestorWaitsForAChildEffectCancelsNavigation() {
        fixture().use { f ->
            val a = f.top()
            val parent = f.child(a, "left", "Parent")
            val nested = f.child(parent, "detail", "Nested")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested"),
            )
            val host = f.activity
            val job = assertNotNull(f.main { host.navigationJob })
            var b: TestNode? = null
            val next = f.main {
                if (animationsEnabled) {
                    f.onNextTransitionStart(nested) { b = f.push(f.root, "B") }
                }
                f.push(nested.chain, "Next").also { next ->
                    if (!animationsEnabled) {
                        f.onNextViewCreated(next) { b = f.push(f.root, "B") }
                    }
                }
            }
            val description = if (animationsEnabled) {
                "the ancestor waits for its running child effect before host destruction"
            } else {
                "the ancestor stack is accepted after child view creation before host destruction"
            }
            f.await(description) {
                if (b != null && f.root.stackFlow.value.lastOrNull() === b &&
                    (!animationsEnabled ||
                        a.state == NavigationNodeState.RESUMED && f.hasResumedView(a) &&
                        f.hasUnresumedView(next) && f.activeTransitionCount > 0)
                ) {
                    host.finish()
                    true
                } else {
                    false
                }
            }
            assertNotNull(f.main { b }, "The child callback must push the ancestor")

            f.closeActivity()

            f.await("host destruction cancels the waiting navigation and destroys all views") {
                host.isDestroyed && !job.isActive && f.allViewsDestroyed
            }
        }
    }

    @Test
    fun ancestorWaitingForCapturedChildEffectSurvivesHostStopAndResume() {
        interruptNestedTransitionWithAncestor(TestEffects.FRAMEWORK, beforePreDraw = true, coverHost = true)
    }

    private fun interruptNestedTransitionWithAncestor(
        effects: TestEffects,
        beforePreDraw: Boolean,
        coverHost: Boolean = false,
    ) {
        fixture(effects).use { f ->
            val host = f.activity
            val root = f.root
            val job = assertNotNull(f.main { host.navigationJob })
            val parent = f.child(f.top(), "left", "Parent")
            val nested = f.child(parent, "detail", "Nested")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested"),
            )
            val nestedModel = f.model(nested)
            val previousStarts = f.startedTransitionCount
            var startsWhenAncestorWasPushed = -1
            var pushedAncestor: TestNode? = null
            val pushAncestor = {
                startsWhenAncestorWasPushed = f.startedTransitionCount
                pushedAncestor = f.push(f.root, "B")
                if (coverHost) f.backgroundHost()
            }
            val next = f.main {
                if (animationsEnabled) {
                    if (beforePreDraw) {
                        f.onNextTransitionCapture(nested, pushAncestor)
                    } else {
                        f.onNextTransitionStart(nested, pushAncestor)
                    }
                }
                f.push(nested.chain, "Next").also { next ->
                    if (!animationsEnabled) {
                        if (beforePreDraw) {
                            f.onNextViewCreated(next, pushAncestor)
                        } else {
                            f.onNextResume(next, pushAncestor)
                        }
                    }
                }
            }
            f.await("the child callback triggers its ancestor push") { pushedAncestor != null }
            if (!animationsEnabled) {
                assertEquals(0, startsWhenAncestorWasPushed, "The lifecycle hook must run with no effects")
            } else if (beforePreDraw) {
                assertEquals(previousStarts, startsWhenAncestorWasPushed)
            } else {
                assertTrue(startsWhenAncestorWasPushed > previousStarts)
            }
            if (coverHost) {
                f.await("the host stops after its child callback starts ancestor navigation") {
                    host.lifecycle.currentState == Lifecycle.State.CREATED
                }
                assertTrue(job.isActive)
                f.returnToHost()
                f.await("the host resumes with the same navigation job") {
                    host.lifecycle.currentState == Lifecycle.State.RESUMED
                }
                assertSame(root, f.root)
                assertTrue(job.isActive)
            }

            f.expect(
                ROOT to listOf("A", "B"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested", "Next"),
            )
            assertTrue(nestedModel.scope.isActive)
            if (animationsEnabled && effects.customAnimations) assertTrue(f.customAnimationRequestCount > 0)
            assertSame(assertNotNull(pushedAncestor), f.pop(f.root))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested", "Next"),
            )
            f.awaitDestroyed(assertNotNull(pushedAncestor))
            assertSame(next, f.top(nested.chain))
            assertSame(nestedModel, f.model(nested))
            assertSame(next, f.pop(nested.chain))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested"),
            )
            f.awaitDestroyed(next)
        }
    }

    private fun nestedPushAndPopSmoke(effects: TestEffects) {
        fixture(effects).use { f ->
            val parent = f.child(f.top(), "left", "Parent")
            val nested = f.child(parent, "detail", "Nested")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested"),
            )
            val next = f.push(nested.chain, "Next")
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested", "Next"),
            )
            assertSame(next, f.pop(nested.chain))
            f.expect(
                ROOT to listOf("A"),
                "left" to listOf("Parent"),
                "detail" to listOf("Nested"),
            )
            f.awaitDestroyed(next)
            f.main {
                when (if (animationsEnabled) effects else TestEffects.NONE) {
                    TestEffects.FRAMEWORK, TestEffects.FRAMEWORK_WITH_CUSTOM_ANIMATIONS ->
                        assertTrue(f.startedTransitionCount > 0)
                    TestEffects.CUSTOM_ANIMATIONS -> assertTrue(f.customAnimationRequestCount > 0)
                    TestEffects.NONE -> {
                        assertEquals(0, f.startedTransitionCount)
                        assertEquals(0, f.customAnimationRequestCount)
                    }
                }
            }
        }
    }

    private fun awaitedDropsSupportAllOverloadsInSequence(awaitDestroy: Boolean) {
        fixture().use { f ->
            val a = f.top()
            val model = f.model(a)
            val b = f.push(f.root, "B")
            f.expect(ROOT to listOf("A", "B"))
            val c = f.push(f.root, "C")
            f.expect(ROOT to listOf("A", "B", "C"))
            val d = f.push(f.root, "D")
            f.expect(ROOT to listOf("A", "B", "C", "D"))

            val removal = f.main {
                f.activity.lifecycleScope.async {
                    if (awaitDestroy) f.root.dropAndAwaitDestroy(b) else f.root.dropAndAwaitAbsence(b)
                    assertEquals(listOf("A", "C", "D"), f.root.labels())
                    if (awaitDestroy) assertEquals(NavigationNodeState.NEW, b.state)

                    assertSame(c, if (awaitDestroy) f.root.dropAndAwaitDestroy(c.id) else f.root.dropAndAwaitAbsence(c.id))
                    assertEquals(listOf("A", "D"), f.root.labels())
                    if (awaitDestroy) assertEquals(NavigationNodeState.NEW, c.state)

                    assertSame(d, if (awaitDestroy) f.root.dropAndAwaitDestroy(d.id.string) else f.root.dropAndAwaitAbsence(d.id.string))
                    assertEquals(listOf("A"), f.root.labels())
                    if (awaitDestroy) {
                        assertEquals(NavigationNodeState.NEW, d.state)
                        f.root.dropAndAwaitDestroy(d)
                        assertNull(f.root.dropAndAwaitDestroy(c.id))
                        assertNull(f.root.dropAndAwaitDestroy(d.id.string))
                    } else {
                        f.root.dropAndAwaitAbsence(d)
                        assertNull(f.root.dropAndAwaitAbsence(c.id))
                        assertNull(f.root.dropAndAwaitAbsence(d.id.string))
                    }
                }
            }
            f.await("all awaited drops complete in order") { removal.isCompleted }
            runBlocking { removal.await() }

            // Stack absence and node destruction do not imply the Fragment's exit effect has finished.
            f.expect(ROOT to listOf("A"))
            listOf(b, c, d).forEach(f::awaitDestroyed)
            assertSame(a, f.top())
            assertSame(model, f.model(a))
            assertTrue(model.scope.isActive)
        }
    }

    private fun awaitedDropWaitsForBeforePauseGate(awaitDestroy: Boolean) {
        fixture().use { f ->
            val b = f.push(f.root, "B")
            f.expect(ROOT to listOf("A", "B"))
            val model = f.model(b)
            val removal = f.main {
                b.useBeforePauseWait = true
                f.activity.lifecycleScope.async {
                    if (awaitDestroy) f.root.dropAndAwaitDestroy(b.id) else f.root.dropAndAwaitAbsence(b.id)
                }
            }
            try {
                f.await("the removed node is waiting before pause") {
                    b.beforePauseWaitJobState.value != null
                }
                f.main {
                    assertFalse(removal.isCompleted)
                    assertEquals(listOf("A", "B"), f.root.labels())
                    assertEquals(NavigationNodeState.RESUMED, b.state)
                    assertTrue(f.hasResumedView(b))
                    assertTrue(model.scope.isActive)
                    assertTrue(assertNotNull(b.beforePauseWaitJobState.value).complete())
                }
                f.await("the awaited drop completes after releasing pause") { removal.isCompleted }
                assertSame(b, runBlocking { removal.await() })
                f.main {
                    assertEquals(listOf("A"), f.root.labels())
                    if (awaitDestroy) assertEquals(NavigationNodeState.NEW, b.state)
                }
                f.expect(ROOT to listOf("A"))
                f.awaitDestroyed(b)
            } finally {
                f.main {
                    b.beforePauseWaitJobState.value?.complete()
                    removal.cancel()
                }
            }
        }
    }

    private fun fixture(
        effects: TestEffects = TestEffects.FRAMEWORK,
        withLoadingFragment: Boolean = true,
        readNodeInOnCreate: Boolean = false,
        withCustomFragmentFactory: Boolean = false,
    ) = NavigationFixture(
        effects = if (animationsEnabled) effects else TestEffects.NONE,
        withLoadingFragment = withLoadingFragment,
        readNodeInOnCreate = readNodeInOnCreate,
        withCustomFragmentFactory = withCustomFragmentFactory,
    )

    private fun pushAndInterruptHost(
        f: NavigationFixture,
        effectDescription: String,
        runningEffect: () -> Boolean,
        interruptHost: () -> Unit,
    ): TestNode {
        var viewCreationObserved = false
        val next = f.main {
            f.push(f.root, "B").also { next ->
                if (!animationsEnabled) {
                    f.onNextViewCreated(next) {
                        viewCreationObserved = true
                        interruptHost()
                    }
                }
            }
        }
        f.await(if (animationsEnabled) effectDescription else "the incoming view triggers host interruption") {
            if (!animationsEnabled) {
                viewCreationObserved
            } else if (runningEffect()) {
                interruptHost()
                true
            } else {
                false
            }
        }
        if (!animationsEnabled) {
            assertTrue(f.main { viewCreationObserved }, "The incoming view creation hook must interrupt the host")
        }
        return next
    }

    private fun populateRestorableTree(f: NavigationFixture) {
        val a = f.top()
        val left = f.child(a, "left", "Left")
        f.child(a, "right", "Right")
        f.expect(
            ROOT to listOf("A"),
            "left" to listOf("Left"),
            "right" to listOf("Right"),
        )
        val leftTop = f.push(left.chain, "Left2")
        f.expect(
            ROOT to listOf("A"),
            "left" to listOf("Left", "Left2"),
            "right" to listOf("Right"),
        )
        val detail = f.child(leftTop, "detail", "Detail")
        expectRestoredTree(f)
        f.push(detail.chain, "Transient", storable = false)
        f.expect(
            ROOT to listOf("A"),
            "left" to listOf("Left", "Left2"),
            "right" to listOf("Right"),
            "detail" to listOf("Detail", "Transient"),
        )
    }

    private fun expectRestoredTree(f: NavigationFixture) {
        f.expect(
            ROOT to listOf("A"),
            "left" to listOf("Left", "Left2"),
            "right" to listOf("Right"),
            "detail" to listOf("Detail"),
        )
        f.main {
            val a = f.top()
            assertEquals("A", a.config.label)
            assertEquals(2, a.subchains.size)
            val childrenByTag = a.subchains.associateBy {
                (it.stackFlow.value.first().config as TestConfig).id
            }
            assertEquals(setOf("left", "right"), childrenByTag.keys)
            val left = childrenByTag.getValue("left")
            val right = childrenByTag.getValue("right")
            assertSame(a, left.parentNode)
            assertSame(a, right.parentNode)

            val leftTop = f.top(left)
            assertEquals("Left2", leftTop.config.label)
            val detail = leftTop.subchains.single()
            assertEquals("detail", (detail.stackFlow.value.first().config as TestConfig).id)
            assertSame(leftTop, detail.parentNode)
            val detailTop = f.top(detail)
            assertEquals("Detail", detailTop.config.label)
            assertEquals(NavigationNodeState.RESUMED, detailTop.state)
            assertTrue(f.hasResumedView(detailTop))
        }
    }

    private fun TestChain.labels(): List<String> =
        stackFlow.value.map { (it.config as TestConfig).label }

    private fun ConfigHolder<NavigationNodeDefaultConfig>.savedLabels(): List<String> = when (this) {
        is ConfigHolder.Chain -> firstNodeConfig?.savedLabels().orEmpty()
        is ConfigHolder.Node -> listOf((config as TestConfig).label) +
            subnode?.savedLabels().orEmpty() + subchains.flatMap { it.savedLabels() }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "animations={0}")
        fun animationModes(): List<Array<Boolean>> = listOf(arrayOf(true), arrayOf(false))
    }
}
