package dev.inmo.navigation.core

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.inmo.micro_utils.common.rootView
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.AndroidNavigationNodeFactory
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.repo.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


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
