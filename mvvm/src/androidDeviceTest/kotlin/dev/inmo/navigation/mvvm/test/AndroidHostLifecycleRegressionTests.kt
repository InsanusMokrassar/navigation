package dev.inmo.navigation.mvvm.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import dev.inmo.navigation.core.AndroidNavigationHost
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.core.initNavigation as initCoreNavigation
import dev.inmo.navigation.core.navigationTag
import dev.inmo.navigation.core.prepareNavigationRestore
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.mvvm.NavigationMVVMSingleActivity
import dev.inmo.navigation.mvvm.ViewModel
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/** Exercises production host entry points without NavigationTestActivity's lifecycle workarounds. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AndroidHostLifecycleRegressionTests {
    @Test
    fun publicExtensionHostRestoresFragmentsWithoutBlockingNodeOrConfigGetters() {
        HostLifecycleFixture(probeRestoredGetters = true).use { f ->
            f.awaitPublicHost()
            assertTrue(f.main { f.session.boundLifecycleReads > 0 }, "First-launch lifecycle getters must work")
            val oldActivity = f.activity
            val unrelated = f.main {
                UnrelatedTestFragment().also { fragment ->
                    oldActivity.supportFragmentManager.beginTransaction()
                        .add(fragment, "unrelated").commitNow()
                    fragment.savedMessage = "preserved across public host recreation"
                }
            }

            f.recreate()
            f.awaitPublicHost()

            assertNotSame(oldActivity, f.activity)
            assertEquals(1, f.main { f.session.savedStateRecreations })
            f.main {
                val restored = assertIs<UnrelatedTestFragment>(
                    f.activity.supportFragmentManager.findFragmentByTag("unrelated"),
                )
                assertNotSame(unrelated, restored)
                assertEquals("preserved across public host recreation", restored.savedMessage)
            }
            val probes = f.main { f.session.getterProbes.toList() }
            // A fix may bind restored instances or discard them before onCreate. Both are valid.
            val diagnostics = probes.map { probe ->
                val eventuallyResolved = probe.completedSuccessfully()
                "eventuallyResolved=$eventuallyResolved, ${probe.diagnostic()}"
            }
            assertTrue(
                probes.isEmpty(),
                "Restored fragments must be bound before onCreate reads node/config. A later binding cannot " +
                    "unblock a synchronous getter on Main: ${diagnostics.joinToString()}",
            )
        }
    }

    @Test
    fun publicExtensionWithoutPreparationFailsUnboundGettersPromptly() {
        HostLifecycleFixture(probeRestoredGetters = true, prepareRestoration = false).use { f ->
            f.awaitPublicHost()
            f.recreate()
            f.awaitPublicHost()

            val probes = f.main { f.session.getterProbes.toList() }
            assertEquals(2, probes.size, "The restored unbound Fragment must probe both getters")
            probes.forEach { it.assertUnboundFailure() }
        }
    }

    @Test
    fun unboundNodeAndConfigAccessFailsPromptly() {
        val fragment = object : NodeFragment<TestConfig, NavigationNodeDefaultConfig>() {
            fun readNode() = node
            fun readConfig() = config
        }
        BoundedGetterProbe("node") { fragment.readNode() }.use { it.assertUnboundFailure() }
        BoundedGetterProbe("config") { fragment.readConfig() }.use { it.assertUnboundFailure() }
    }

    @Test
    fun destroyedActivityDoesNotRunDelayedProductionNavigationStartup() {
        HostLifecycleFixture(delayedStartup = true).use { f ->
            f.awaitStartupEntered()
            val activity = f.activity as DelayedProductionNavigationActivity
            assertEquals(0, f.main { f.session.initialConfigCallsAfterDestroy })

            f.closeActivity()
            assertTrue(f.main { activity.isDestroyed })
            f.session.startupRelease.complete(Unit)
            f.awaitStartupFinished()

            val state = f.main {
                Triple(
                    f.session.initialConfigCallsAfterDestroy,
                    activity.navigationHost != null,
                    activity.navigationHost?.job?.isActive == true,
                )
            }
            assertTrue(
                state.first == 0 && !state.second && !state.third,
                "Destroyed Activity must cancel pending startup before creating a host; " +
                    "initialConfigCallsAfterDestroy=${state.first}, hostCreated=${state.second}, " +
                    "navigationJobActive=${state.third}",
            )
        }
    }

    @Test
    fun finishingHostCancelsViewModelScopeAndItsRunningWork() {
        HostLifecycleFixture().use { f ->
            f.awaitPublicHost()
            val models = f.main { f.session.models.toList() }
            val navigationJob = f.main { checkNotNull(f.session.hosts.last().job) }
            f.awaitModelWorkStarted(models)
            assertTrue(models.isNotEmpty())
            assertTrue(models.all { it.scope.isActive && it.work.isActive })

            f.closeActivity()

            assertFalse(navigationJob.isActive, "The host itself must finish before checking model cleanup")
            f.assertModelsStopped(models, "Activity finish")
        }
    }

    @Test
    fun recreatingHostCancelsOldViewModelsAndKeepsReplacementModelsActive() {
        HostLifecycleFixture().use { f ->
            f.awaitPublicHost()
            val oldModels = f.main { f.session.models.toList() }
            val oldNavigationJob = f.main { checkNotNull(f.session.hosts.last().job) }
            f.awaitModelWorkStarted(oldModels)
            val oldActivity = f.activity

            f.recreate()
            f.awaitPublicHost()

            assertNotSame(oldActivity, f.activity)
            assertFalse(oldNavigationJob.isActive, "The old host must stop when its Activity is recreated")
            val replacementModels = f.main { f.session.models.filter { it !in oldModels } }
            assertTrue(replacementModels.isNotEmpty(), "Recreation must construct a fresh navigation hierarchy")
            f.awaitModelWorkStarted(replacementModels)
            assertTrue(replacementModels.all { it.scope.isActive && it.work.isActive })
            f.assertModelsStopped(oldModels, "Activity recreation")
        }
    }
}

internal class HostLifecycleSession(val probeRestoredGetters: Boolean, val prepareRestoration: Boolean) {
    val repo = NavigationConfigsRepo.InMemory<NavigationNodeDefaultConfig>()
    val activities = mutableListOf<AppCompatActivity>()
    val hosts = mutableListOf<AndroidNavigationHost<NavigationNodeDefaultConfig>>()
    val fragments = mutableListOf<HostLifecycleFragment>()
    val models = mutableListOf<HostLifecycleViewModel>()
    val getterProbes = mutableListOf<BoundedGetterProbe>()
    var boundLifecycleReads = 0
    var savedStateRecreations = 0
    var initialConfigCallsAfterDestroy = 0
    val startupEntered = CompletableDeferred<Unit>()
    val startupRelease = CompletableDeferred<Unit>()
    val startupFinished = CompletableDeferred<Unit>()
}

private object HostLifecycleSessionHolder {
    var session: HostLifecycleSession? = null
    fun requireSession() = checkNotNull(session)
}

/** Deliberately uses the public extension on an ordinary AppCompatActivity. */
class PublicExtensionNavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val session = HostLifecycleSessionHolder.requireSession()
        session.activities += this
        if (savedInstanceState != null) session.savedStateRecreations++
        if (session.prepareRestoration) prepareNavigationRestore()
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply {
            id = CONTAINER_ID
            navigationTag = CONTAINER_TAG
        })
        session.hosts += initCoreNavigation<NavigationNodeDefaultConfig>(
            startConfig = TestConfig(CONTAINER_TAG, "Root"),
            configsRepo = session.repo,
            manualHierarchyCheckerDelayMillis = 20L,
            fragmentsClassesFactory = { HostLifecycleFragment::class },
        )
    }

    private companion object {
        const val CONTAINER_ID = 0x00abcd01
        const val CONTAINER_TAG = "host-lifecycle-regression"
    }
}

/** The barrier controls timing only; the base implementation chooses the scope and starts the host. */
class DelayedProductionNavigationActivity : NavigationMVVMSingleActivity() {
    val navigationHost get() = currentAndroidNavigationHost

    override fun onCreate(savedInstanceState: Bundle?) {
        HostLifecycleSessionHolder.requireSession().activities += this
        super.onCreate(savedInstanceState)
    }

    override fun createInitialConfig(rootFragmentTag: String): NavigationNodeDefaultConfig {
        if (isDestroyed) HostLifecycleSessionHolder.requireSession().initialConfigCallsAfterDestroy++
        return TestConfig(rootFragmentTag, "Root")
    }

    override suspend fun onStartNavigation(rootFragmentTag: String) {
        val session = HostLifecycleSessionHolder.requireSession()
        session.startupEntered.complete(Unit)
        try {
            session.startupRelease.await()
            super.onStartNavigation(rootFragmentTag)
        } finally {
            session.startupFinished.complete(Unit)
        }
    }
}

internal class HostLifecycleFragment : NodeFragment<TestConfig, NavigationNodeDefaultConfig>() {
    private val session = HostLifecycleSessionHolder.requireSession()
    private var boundNode: TestNode? = null

    override fun onSetNode(node: TestNode, configStateFlow: MutableStateFlow<TestConfig>) {
        super.onSetNode(node, configStateFlow)
        boundNode = node
        session.models += HostLifecycleViewModel(node)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session.fragments += this
        if (boundNode != null) {
            check(node === boundNode && config === boundNode?.config)
            session.boundLifecycleReads++
        } else if (session.probeRestoredGetters) {
            // Bound the failure if a getter regresses to waiting: Main must remain responsive,
            // and cleanup can interrupt the worker instead of leaving the instrumentation in an ANR.
            session.getterProbes += BoundedGetterProbe("node") { node }
            session.getterProbes += BoundedGetterProbe("config") { config }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext())

    internal fun isBoundAndResumed() = boundNode?.state == NavigationNodeState.RESUMED && isResumed
}

internal class HostLifecycleViewModel(node: TestNode) : ViewModel<NavigationNodeDefaultConfig>(node) {
    val workStarted = CompletableDeferred<Unit>()
    val workStopped = CompletableDeferred<Unit>()
    val work = scope.launch {
        workStarted.complete(Unit)
        try {
            awaitCancellation()
        } finally {
            workStopped.complete(Unit)
        }
    }
}

internal class BoundedGetterProbe(private val name: String, read: () -> Any?) : Closeable {
    private val done = CountDownLatch(1)
    private val result = AtomicReference<Result<Any?>?>()
    private val thread = Thread({
        try {
            result.set(runCatching(read))
        } finally {
            done.countDown()
        }
    }, "restored-fragment-$name-getter").apply {
        isDaemon = true
        start()
    }

    fun completedSuccessfully() = done.await(1, TimeUnit.SECONDS) && result.get()?.isSuccess == true

    fun assertUnboundFailure() {
        assertTrue(done.await(1, TimeUnit.SECONDS), "An unbound getter must not wait: ${diagnostic()}")
        val failure = assertIs<IllegalStateException>(result.get()?.exceptionOrNull(), diagnostic())
        assertTrue(failure.message.orEmpty().contains("prepareNavigationRestore()"), diagnostic())
        assertTrue(failure.message.orEmpty().contains("before super.onCreate"), diagnostic())
    }

    fun diagnostic() = "$name: thread=${thread.state}, result=${result.get()}, " +
        "stack=${thread.stackTrace.take(6).joinToString(" <- ")}"

    override fun close() {
        thread.interrupt()
        thread.join(1_000)
        check(!thread.isAlive) { "Getter probe did not stop: ${diagnostic()}" }
    }
}

private class HostLifecycleFixture(
    probeRestoredGetters: Boolean = false,
    delayedStartup: Boolean = false,
    prepareRestoration: Boolean = true,
) : Closeable {
    val session = HostLifecycleSession(probeRestoredGetters, prepareRestoration)
    private var scenario: ActivityScenario<Activity>? = null

    init {
        main {
            check(HostLifecycleSessionHolder.session == null)
            HostLifecycleSessionHolder.session = session
            startKoin {
                modules(module {
                    single<NavigationFragmentInfoProvider> {
                        object : NavigationFragmentInfoProvider {
                            override val configsKClasses = setOf(TestConfig::class)
                            override fun resolveFragmentKClass(config: NavigationNodeDefaultConfig) =
                                HostLifecycleFragment::class.takeIf { config is TestConfig }
                        }
                    }
                })
            }
        }
        try {
            val context = InstrumentationRegistry.getInstrumentation().context
            // The production MVVM entry point uses this preference file by default.
            context.getSharedPreferences("internal", Context.MODE_PRIVATE).edit().clear().commit()
            scenario = ActivityScenario.launch(Intent(
                context,
                if (delayedStartup) DelayedProductionNavigationActivity::class.java
                else PublicExtensionNavigationActivity::class.java,
            ))
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    val activity get() = main { session.activities.last() }

    fun awaitPublicHost() = runBlocking {
        withTimeout(5_000) {
            while (!main {
                session.hosts.lastOrNull()?.job?.isActive == true &&
                    session.fragments.any { it.activity === session.activities.last() && it.isBoundAndResumed() }
            }) delay(10)
        }
    }

    fun awaitStartupEntered() = runBlocking { withTimeout(5_000) { session.startupEntered.await() } }
    fun awaitStartupFinished() = runBlocking { withTimeout(5_000) { session.startupFinished.await() } }

    fun awaitModelWorkStarted(models: List<HostLifecycleViewModel>) = runBlocking {
        withTimeout(5_000) { models.forEach { it.workStarted.await() } }
    }

    fun assertModelsStopped(models: List<HostLifecycleViewModel>, trigger: String) {
        val workStopped = runBlocking {
            withTimeoutOrNull(1_000) { models.forEach { it.work.join() }; true } == true
        }
        assertTrue(
            workStopped && models.all { !it.scope.isActive && it.work.isCompleted && it.workStopped.isCompleted },
            "$trigger must cancel old ViewModels before fixture cleanup; " +
                "activeScopes=${models.count { it.scope.isActive }}, activeWorkers=${models.count { it.work.isActive }}",
        )
    }

    fun recreate() = checkNotNull(scenario).recreate()

    fun closeActivity() {
        scenario?.close()
        scenario = null
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
            session.startupRelease.complete(Unit)
            if (session.startupEntered.isCompleted) awaitStartupFinished()
        } finally {
            val probes = main {
                session.hosts.forEach { it.job?.cancel() }
                session.activities.filterIsInstance<DelayedProductionNavigationActivity>()
                    .forEach { it.navigationHost?.job?.cancel() }
                // Deliberately after the assertions: this cleanup must not make leak regressions pass.
                session.models.forEach { it.scope.cancel() }
                session.getterProbes.toList()
            }
            try {
                probes.forEach { it.close() }
            } finally {
                main {
                    stopKoin()
                    HostLifecycleSessionHolder.session = null
                }
            }
        }
    }
}
