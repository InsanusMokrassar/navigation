package dev.inmo.navigation.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.activity.contextaware.OnContextAvailableListener
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.inmo.micro_utils.common.rootView
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.AndroidNavigationNodeFactory
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.repo.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Prepares Fragment restoration for an Activity using [initNavigation]. Call this before
 * `super.onCreate(savedInstanceState)` on every Activity creation, or from the Activity initializer.
 * [initNavigation] itself is called after the Activity's content view is ready.
 *
 * Navigation restores its hierarchy from [NavigationConfigsRepo] and binds fresh [NodeFragment]
 * instances. Discard FragmentManager's restored navigation instances before their lifecycle starts;
 * other Fragments and the Activity's FragmentFactory retain their normal restoration behavior.
 *
 * @throws IllegalStateException if called after the Activity's context has become available during
 * `super.onCreate`, when restored Fragment lifecycle callbacks can already have run.
 */
fun AppCompatActivity.prepareNavigationRestore() {
    check(peekAvailableContext() == null) {
        "prepareNavigationRestore() must be called before super.onCreate(savedInstanceState)."
    }
    addOnContextAvailableListener(object : OnContextAvailableListener {
        override fun onContextAvailable(context: Context) {
            removeOnContextAvailableListener(this)
            // FragmentActivity's earlier context listener restores its instances before this runs.
            val restoredNodes = supportFragmentManager.fragments.filterIsInstance<NodeFragment<*, *>>()
            if (restoredNodes.isNotEmpty()) {
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    restoredNodes.forEach { remove(it) }
                }.commitNow()
            }
        }
    })
}

open class AndroidNavigationHost<T : NavigationNodeDefaultConfig>(
    protected val configsRepo: NavigationConfigsRepo<T>,
    protected val startChain: ConfigHolder<T>,
    protected val manualHierarchyCheckerDelayMillis: Long? = 100L,
    protected val dropRedundantChainsOnRestore: Boolean = false,
    protected val fragmentTransactionConfigurator: FragmentTransactionConfigurator<T>? = null,
    protected val fragmentsClassesFactory: FragmentsClassesFactory<T>
) {
    var job: Job? = null
        private set
    protected val jobMutex = Mutex()

    fun start(
        scope: CoroutineScope,
        fragmentManager: FragmentManager,
        rootView: View,
        flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener = FlowOnHierarchyChangeListener(recursive = true).also {
            (rootView as ViewGroup).setOnHierarchyChangeListenerRecursively(it)
        }
    ) {
        scope.launchSafelyWithoutExceptions {
            jobMutex.withLock {
                val subscope = scope.LinkedSupervisorScope()
                job ?.cancel()
                job = subscope.coroutineContext.job
                val surrogateFactory = AndroidNavigationNodeFactory<T>(
                    fragmentManager,
                    rootView,
                    flowOnHierarchyChangeListener,
                    manualHierarchyCheckerDelayMillis,
                    fragmentTransactionConfigurator,
                    fragmentsClassesFactory
                )
                val nodesFactory = NavigationNodeFactory<T> { navigationChain, config ->
                    surrogateFactory.createNode(navigationChain, config) ?.also {
                        it.chain.enableSavingHierarchy(
                            configsRepo,
                            subscope
                        )
                    }
                }

                restoreHierarchy<T>(
                    configsRepo.get() ?: startChain,
                    factory = nodesFactory,
                    dropRedundantChainsOnRestore = dropRedundantChainsOnRestore
                ) ?.start(subscope)
            }
        }
    }
}

/**
 * Starts navigation after the Activity's content view is ready. Call [prepareNavigationRestore]
 * before `super.onCreate(savedInstanceState)` so restored navigation Fragments cannot receive
 * lifecycle callbacks before their nodes are bound.
 */
inline fun <reified T : NavigationNodeDefaultConfig> AppCompatActivity.initNavigation(
    startChain: ConfigHolder<T>,
    configsRepo: NavigationConfigsRepo<T> = AndroidSPConfigsRepo(
        getSharedPreferences("internal", AppCompatActivity.MODE_PRIVATE),
        T::class
    ),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    fragmentManager: FragmentManager = supportFragmentManager,
    rootView: View = this.rootView!!,
    flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener = FlowOnHierarchyChangeListener(recursive = true).also {
        (rootView as ViewGroup).setOnHierarchyChangeListenerRecursively(it)
    },
    manualHierarchyCheckerDelayMillis: Long? = 100L,
    dropRedundantChainsOnRestore: Boolean = false,
    fragmentTransactionConfigurator: FragmentTransactionConfigurator<T>? = null,
    noinline fragmentsClassesFactory: FragmentsClassesFactory<T>
) = AndroidNavigationHost(
    configsRepo,
    startChain,
    manualHierarchyCheckerDelayMillis,
    dropRedundantChainsOnRestore,
    fragmentTransactionConfigurator,
    fragmentsClassesFactory
).apply {
    start(scope, fragmentManager, rootView, flowOnHierarchyChangeListener)
    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            job ?.cancel()
        }
    }
    lifecycle.addObserver(observer)
    job ?.invokeOnCompletion {
        lifecycle.removeObserver(observer)
    }
}

/**
 * Starts navigation after the Activity's content view is ready. Call [prepareNavigationRestore]
 * before `super.onCreate(savedInstanceState)` so restored navigation Fragments cannot receive
 * lifecycle callbacks before their nodes are bound.
 */
inline fun <reified T : NavigationNodeDefaultConfig> AppCompatActivity.initNavigation(
    startConfig: T,
    configsRepo: NavigationConfigsRepo<T> = AndroidSPConfigsRepo(
        getSharedPreferences("internal", AppCompatActivity.MODE_PRIVATE),
        T::class
    ),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    fragmentManager: FragmentManager = supportFragmentManager,
    rootView: View = this.rootView!!,
    flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener = FlowOnHierarchyChangeListener(recursive = true).also {
        (rootView as ViewGroup).setOnHierarchyChangeListenerRecursively(it)
    },
    manualHierarchyCheckerDelayMillis: Long? = 100L,
    dropRedundantChainsOnRestore: Boolean = false,
    fragmentTransactionConfigurator: FragmentTransactionConfigurator<T>? = null,
    noinline fragmentsClassesFactory: FragmentsClassesFactory<T>
) = initNavigation(
    ConfigHolder.Chain(
        ConfigHolder.Node(
            startConfig,
            null,
            emptyList()
        )
    ),
    configsRepo,
    scope,
    fragmentManager,
    rootView,
    flowOnHierarchyChangeListener,
    manualHierarchyCheckerDelayMillis,
    dropRedundantChainsOnRestore,
    fragmentTransactionConfigurator,
    fragmentsClassesFactory
)
