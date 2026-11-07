package dev.inmo.navigation.mvvm.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.transition.Fade
import android.transition.Slide
import android.transition.Transition
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.AndroidFragmentNode
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.fragments.view.NavigationFragmentContainerView
import dev.inmo.navigation.core.navigationTag
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.mvvm.NavigationFragmentComposePlace
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.mvvm.NavigationMVVMSingleActivity
import dev.inmo.navigation.mvvm.ViewFragment
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.navigation.mvvm.initNavigation
import java.io.Closeable
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

internal typealias TestNode = AndroidFragmentNode<TestConfig, NavigationNodeDefaultConfig>
internal typealias TestChain = NavigationChain<NavigationNodeDefaultConfig>

internal enum class TestEffects { FRAMEWORK, CUSTOM_ANIMATIONS, NONE }

@Serializable
internal data class TestConfig(
    override val id: String,
    val label: String,
    override val storableInNavigationHierarchy: Boolean = true,
) : NavigationNodeDefaultConfig

internal class TestNavigationViewModel(node: TestNode) : ViewModel<NavigationNodeDefaultConfig>(node)

/** Each read deserializes a new tree, including when the Activity is recreated. */
private class SerializedConfigsRepo : NavigationConfigsRepo<NavigationNodeDefaultConfig> {
    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(NavigationNodeDefaultConfig::class) { subclass(TestConfig::class) }
        }
    }
    private val serializer = ConfigHolder.serializer(PolymorphicSerializer(NavigationNodeDefaultConfig::class))
    private var saved: String? = null

    override fun save(holder: ConfigHolder<NavigationNodeDefaultConfig>) {
        saved = json.encodeToString(serializer, holder)
    }

    override fun get(): ConfigHolder<NavigationNodeDefaultConfig>? =
        saved?.let { json.decodeFromString(serializer, it) }
}

private class ViewRecord(val node: TestNode?, val view: View, var destroyed: Boolean = false)

private class TestSession(val effects: TestEffects) {
    val repo = SerializedConfigsRepo()
    var root: TestChain? = null
    var activity: NavigationTestActivity? = null
    val fragments = mutableMapOf<NavigationNodeId, NavigationTestFragment>()
    val models = mutableMapOf<NavigationNodeId, TestNavigationViewModel>()
    val views = mutableListOf<ViewRecord>()
    val transitions: MutableSet<Transition> = Collections.newSetFromMap(IdentityHashMap())
    var startedTransitions = 0
    var customAnimationRequests = 0
}

private object TestHost {
    var session: TestSession? = null
    fun requireSession(): TestSession = checkNotNull(session)
}

/** Uses the public MVVM entry point and ordinary asynchronous Fragment transactions. */
class NavigationTestActivity : NavigationMVVMSingleActivity() {
    val navigationJob: Job?
        get() = currentAndroidNavigationHost?.job

    override fun onCreate(savedInstanceState: Bundle?) {
        TestHost.requireSession().apply {
            root = null
            activity = this@NavigationTestActivity
        }
        @Suppress("DEPRECATION") // Also supports API 26; applies only to this test window.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        super.onCreate(savedInstanceState)
    }

    override fun allocateLoadingFragment(): Fragment = Fragment()

    override fun createInitialConfig(rootFragmentTag: String): NavigationNodeDefaultConfig =
        TestConfig(rootFragmentTag, "A")

    override suspend fun onStartNavigation(rootFragmentTag: String) {
        doInUI {
            if (isFinishing || isDestroyed) return@doInUI
            val session = TestHost.requireSession()
            currentAndroidNavigationHost?.job?.cancel()
            currentAndroidNavigationHost = initNavigation<NavigationNodeDefaultConfig>(
                startConfig = createInitialConfig(rootFragmentTag),
                navigationFragmentInfoProviders = listOf(object : NavigationFragmentInfoProvider {
                    override val configsKClasses = setOf(TestConfig::class)
                    override fun resolveFragmentKClass(config: NavigationNodeDefaultConfig) =
                        NavigationTestFragment::class.takeIf { config is TestConfig }
                }),
                configsRepo = session.repo,
                scope = lifecycleScope,
                manualHierarchyCheckerDelayMillis = 20L,
                dropRedundantChainsOnRestore = true,
                fragmentTransactionConfigurator = if (session.effects == TestEffects.CUSTOM_ANIMATIONS) {
                    FragmentTransactionConfigurator.Default<NavigationNodeDefaultConfig> {
                        setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out)
                    }
                } else null,
            )
        }
    }
}

internal class NavigationTestFragment : ViewFragment<TestNavigationViewModel, TestConfig>() {
    private val session = TestHost.requireSession()
    private var boundNode: TestNode? = null
    private var viewRecord: ViewRecord? = null
    override val viewModelClass = TestNavigationViewModel::class
    override val contentBoxModifier = Modifier

    override fun onSetNode(node: TestNode, configStateFlow: MutableStateFlow<TestConfig>) {
        super.onSetNode(node, configStateFlow)
        boundNode = node
        session.fragments[node.id] = this
        session.models[node.id] = viewModel
        if (node.chain.parentNode == null) session.root = node.chain
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.effects == TestEffects.FRAMEWORK) {
            // Real framework effects reproduce the original MI 6 failure; AndroidX transitions do not.
            enterTransition = tracked(Slide(Gravity.RIGHT))
            exitTransition = tracked(Fade())
        }
    }

    private fun tracked(transition: Transition): Transition = transition.apply {
        duration = 240L
        addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                session.transitions.add(transition)
                session.startedTransitions++
            }
            override fun onTransitionEnd(transition: Transition) { session.transitions.remove(transition) }
            override fun onTransitionCancel(transition: Transition) { session.transitions.remove(transition) }
            override fun onTransitionPause(transition: Transition) = Unit
            override fun onTransitionResume(transition: Transition) = Unit
        })
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (nextAnim != 0) session.customAnimationRequests++
        // Let FragmentManager load and run the resource, including its own completion listener.
        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    @Composable
    override fun BoxScope.Content() {
        // FragmentManager can restore an unbound fragment before the host loads the serialized tree.
        val currentNode = boundNode ?: return
        Column(Modifier.fillMaxWidth()) {
            BasicText(currentNode.config.label, Modifier.height(32.dp))
            val subchains by currentNode.subchainsFlow.collectAsState()
            for (chain in subchains) {
                key(chain) {
                    val stack by chain.stackFlow.collectAsState()
                    stack.firstOrNull()?.config?.id?.let { tag ->
                        key(tag) { NavigationFragmentComposePlace(tag, Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewRecord = ViewRecord(boundNode, view).also(session.views::add)
    }

    override fun onDestroyView() {
        viewRecord?.destroyed = true
        viewRecord = null
        super.onDestroyView()
    }
}

@Suppress("UNCHECKED_CAST") // This fixture's provider only creates TestConfig/TestNode instances.
internal class NavigationFixture(effects: TestEffects = TestEffects.FRAMEWORK) : Closeable {
    private val session = TestSession(effects)
    private var scenario: ActivityScenario<NavigationTestActivity>? = null

    init {
        main {
            check(TestHost.session == null) { "Another navigation fixture is still running" }
            TestHost.session = session
            startKoin { modules(module { factory { parameters -> TestNavigationViewModel(parameters.get()) } }) }
        }
        try {
            relaunch()
            expect(ROOT to listOf("A"))
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    val root: TestChain get() = main { checkNotNull(session.root) }
    val activity: NavigationTestActivity get() = main { checkNotNull(session.activity) }
    val activeTransitionCount: Int get() = main { session.transitions.size }
    val startedTransitionCount: Int get() = main { session.startedTransitions }
    val customAnimationRequestCount: Int get() = main { session.customAnimationRequests }
    val attachedViewCount: Int get() = main { session.views.count { it.view.isAttachedToWindow } }
    val allViewsDestroyed: Boolean get() = main {
        session.views.all { it.destroyed && it.view.parent == null && !it.view.isAttachedToWindow }
    }

    fun top(chain: TestChain = root): TestNode = main { chain.stackFlow.value.last() as TestNode }

    fun push(chain: TestChain, label: String, storable: Boolean = true): TestNode = main {
        checkNotNull(chain.push(TestConfig(top(chain).config.id, label, storable))) as TestNode
    }

    fun child(parent: TestNode, tag: String, label: String): TestNode = main {
        checkNotNull(parent.createSubChain(TestConfig(tag, label))).first as TestNode
    }

    fun pop(chain: TestChain): TestNode? = main { chain.pop() as TestNode? }
    fun drop(node: TestNode) { main { checkNotNull(node.chain.drop(node)) } }

    fun replace(node: TestNode, label: String, reversed: Boolean = false): TestNode = main {
        val config = TestConfig(node.config.id, label)
        val result = if (reversed) node.chain.reversedReplace(node, config) else node.chain.replace(node, config)
        checkNotNull(result).second as TestNode
    }

    fun model(node: TestNode): TestNavigationViewModel = main { checkNotNull(session.models[node.id]) }
    fun savedHierarchy(): ConfigHolder<NavigationNodeDefaultConfig>? = main { session.repo.get() }

    fun hasResumedView(node: TestNode): Boolean = main {
        session.fragments[node.id]?.let { it.isResumed && it.view?.isAttachedToWindow == true } == true
    }

    fun awaitDestroyed(node: TestNode) = await("${node.config.label} destroyed and its ViewModel cancelled") {
        node.state == NavigationNodeState.NEW && !model(node).scope.isActive &&
            session.views.filter { it.node === node }.all { it.destroyed && it.view.parent == null }
    }

    fun recreate() {
        checkNotNull(scenario).recreate()
        await("the recreated host starts navigation") { session.root != null && activity.navigationJob?.isActive == true }
    }

    fun closeActivity() {
        scenario?.close()
        scenario = null
    }

    fun relaunch() {
        check(scenario == null)
        main { session.root = null }
        scenario = ActivityScenario.launch(
            Intent(InstrumentationRegistry.getInstrumentation().context, NavigationTestActivity::class.java)
        )
        await("the host starts navigation") { session.root != null && activity.navigationJob?.isActive == true }
    }

    /** Model and Android view checks must hold together for two frames, after all effects finish. */
    fun expect(vararg chains: Pair<String, List<String>>) {
        val expected = chains.toMap()
        var mismatch: String? = null
        waitUntil("navigation tree $expected", stableFrames = 2, detail = { mismatch.orEmpty() }) {
            mismatch = mismatch(expected)
            mismatch == null
        }
    }

    private fun mismatch(expected: Map<String, List<String>>): String? {
        val root = session.root ?: return "No root chain"
        val actual = linkedMapOf<String, List<String>>()
        val nodeStates = linkedMapOf<TestNode, NavigationNodeState>()
        fun visit(chain: TestChain, parentState: NavigationNodeState) {
            val stack = chain.stackFlow.value.map { it as TestNode }
            val tag = stack.firstOrNull()?.config?.id ?: ROOT
            check(tag !in actual) { "Duplicate chain tag $tag" }
            actual[tag] = stack.map { it.config.label }
            stack.forEachIndexed { index, node ->
                val state = minOf(parentState, if (index == stack.lastIndex) NavigationNodeState.RESUMED else NavigationNodeState.STARTED)
                nodeStates[node] = state
                node.subchains.forEach { visit(it, state) }
            }
        }
        visit(root, NavigationNodeState.RESUMED)
        if (actual != expected) return "Stacks: $actual"
        nodeStates.forEach { (node, state) ->
            if (node.state != state) return "${node.config.label}: ${node.state}, expected $state"
        }
        val visible = nodeStates.filterValues { it == NavigationNodeState.RESUMED }.keys
        val expectedFragments = visible.map { checkNotNull(session.fragments[it.id]) }.toSet()
        val addedFragments = activity.supportFragmentManager.fragments.filter { it.isAdded }.toSet()
        if (addedFragments != expectedFragments) return "Added fragments: $addedFragments; expected $expectedFragments"
        val visibleViews = visible.map { node ->
            val fragment = checkNotNull(session.fragments[node.id])
            val view = fragment.view
            if (!fragment.isResumed || view == null || !view.isAttachedToWindow || !view.isShown || view.width == 0 || view.height == 0) {
                return "${node.config.label}: resumed=${fragment.isResumed}, view=$view"
            }
            if (view.animation?.hasEnded() == false) return "${node.config.label}: view animation has not finished"
            val container = view.parent as? NavigationFragmentContainerView
                ?: return "${node.config.label}: wrong view parent ${view.parent}"
            if (container.navigationTag != node.config.id || container.childCount != 1 || container.getChildAt(0) !== view) {
                return "${node.config.label}: container=${container.navigationTag}, children=${container.childCount}"
            }
            view
        }.toSet()
        val retired = session.views.filter { it.view !in visibleViews }
        if (retired.any { !it.destroyed || it.view.isAttachedToWindow || it.view.parent != null }) {
            return "Retired views still present: ${retired.filter { !it.destroyed || it.view.parent != null }.map {
                "${it.node?.config?.label}(destroyed=${it.destroyed}, attached=${it.view.isAttachedToWindow}, " +
                    "parent=${it.view.parent}, animationEnded=${it.view.animation?.hasEnded()})"
            }}"
        }
        if (session.transitions.isNotEmpty()) {
            return "Transitions still running: ${session.transitions.size}"
        }
        return null
    }

    fun await(description: String, condition: () -> Boolean) = waitUntil(description, condition = condition)

    /** Observe ordinary UI frames, without committing transactions or draining the main queue. */
    private fun waitUntil(
        description: String,
        stableFrames: Int = 1,
        detail: () -> String = { "" },
        condition: () -> Boolean,
    ) {
        check(Looper.myLooper() != Looper.getMainLooper())
        val completed = CountDownLatch(1)
        val stopped = AtomicBoolean(false)
        val failure = AtomicReference<Throwable?>()
        val deadline = SystemClock.uptimeMillis() + 15_000L
        var consecutive = 0
        lateinit var callback: Choreographer.FrameCallback
        callback = Choreographer.FrameCallback {
            if (!stopped.get()) {
                try {
                    consecutive = if (condition()) consecutive + 1 else 0
                    if (consecutive >= stableFrames) {
                        stopped.set(true)
                        completed.countDown()
                    } else {
                        check(SystemClock.uptimeMillis() < deadline) { "Timed out: $description. ${detail()}" }
                        Choreographer.getInstance().postFrameCallback(callback)
                    }
                } catch (error: Throwable) {
                    failure.set(error)
                    stopped.set(true)
                    completed.countDown()
                }
            }
        }
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post { if (!stopped.get()) Choreographer.getInstance().postFrameCallback(callback) }
        try {
            check(completed.await(20, TimeUnit.SECONDS)) { "No UI frames while waiting for $description" }
            failure.get()?.let { throw it }
        } finally {
            stopped.set(true)
            mainHandler.post { Choreographer.getInstance().removeFrameCallback(callback) }
        }
    }

    fun <T> main(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    override fun close() {
        try {
            closeActivity()
        } finally {
            main {
                // Host destruction stops navigation; retained nodes do not receive onDestroy.
                session.models.values.forEach { it.scope.cancel() }
                stopKoin()
                TestHost.session = null
            }
        }
    }

    companion object {
        const val ROOT = "entrance"
    }
}
