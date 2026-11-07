package dev.inmo.navigation.mvvm.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.mvvm.test.NavigationFixture.Companion.ROOT
import kotlinx.coroutines.isActive
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentNavigationTests {
    @Test
    fun nestedUnstorablePushesAndPopsLeaveSiblingChainIntact() {
        NavigationFixture().use { f ->
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
            assertTrue(f.main { f.startedTransitionCount > 0 })
        }
    }

    @Test
    fun burstPushesAndPopDuringTransitionReachTheFinalStack() {
        NavigationFixture().use { f ->
            f.expect(ROOT to listOf("A"))
            val (b, c) = f.main {
                f.push(f.root, "B") to f.push(f.root, "C")
            }

            // Reverse the accepted stack in the same main-thread turn that observes its running effect.
            f.await("the burst stack is accepted while a framework transition is running") {
                if (f.root.labels() == listOf("A", "B", "C") && f.activeTransitionCount > 0) {
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
        NavigationFixture().use { f ->
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
        NavigationFixture().use { f ->
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
    fun beforePauseGateKeepsTheOutgoingNodeAndViewModelActive() {
        NavigationFixture().use { f ->
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
        NavigationFixture().use { f ->
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
        NavigationFixture().use { f ->
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
        NavigationFixture().use { f ->
            f.expect(ROOT to listOf("A"))
            val host = f.activity
            val navigationJob = assertNotNull(f.main { host.navigationJob })
            assertTrue(navigationJob.isActive)
            f.push(f.root, "B")
            f.await("a framework transition is running before host destruction") {
                if (f.activeTransitionCount > 0) {
                    host.finish()
                    true
                } else {
                    false
                }
            }

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
    fun disabledAnimationsSupportNestedPushAndPop() {
        nestedPushAndPopSmoke(TestEffects.NONE)
    }

    private fun nestedPushAndPopSmoke(effects: TestEffects) {
        NavigationFixture(effects).use { f ->
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
                when (effects) {
                    TestEffects.FRAMEWORK -> assertTrue(f.startedTransitionCount > 0)
                    TestEffects.CUSTOM_ANIMATIONS -> assertTrue(f.customAnimationRequestCount > 0)
                    TestEffects.NONE -> {
                        assertEquals(0, f.startedTransitionCount)
                        assertEquals(0, f.customAnimationRequestCount)
                    }
                }
            }
        }
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
}
