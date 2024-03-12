package dev.inmo.navigation.mvvm

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.common.rootView
import dev.inmo.micro_utils.coroutines.FlowOnHierarchyChangeListener
import dev.inmo.micro_utils.coroutines.setOnHierarchyChangeListenerRecursively
import dev.inmo.navigation.core.AndroidSPConfigsRepo
import dev.inmo.navigation.core.FragmentsClassesFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.initNavigation
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

inline fun <reified T : NavigationNodeDefaultConfig> AppCompatActivity.initNavigation(
    startConfig: T,
    navigationFragmentInfoProviders: List<NavigationFragmentInfoProvider>,
    configsRepo: NavigationConfigsRepo<T> = AndroidSPConfigsRepo(
        getSharedPreferences("internal", AppCompatActivity.MODE_PRIVATE),
        T::class,
        navigationFragmentInfoProviders.flatMap {
            it.configsKClasses.mapNotNull {
                if (it.allSuperclasses.contains(T::class)) {
                    @Suppress("UNCHECKED_CAST")
                    it as KClass<out T>
                } else {
                    null
                }
            }
        }.distinct()
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
    noinline fragmentsClassesFactory: FragmentsClassesFactory<T> = { config ->
        navigationFragmentInfoProviders.firstNotNullOfOrNull { it.resolveFragmentKClass(config) }
    }
) = initNavigation(
    startConfig = startConfig,
    configsRepo = configsRepo,
    scope = scope,
    fragmentManager = fragmentManager,
    rootView = rootView,
    flowOnHierarchyChangeListener = flowOnHierarchyChangeListener,
    manualHierarchyCheckerDelayMillis = manualHierarchyCheckerDelayMillis,
    dropRedundantChainsOnRestore = dropRedundantChainsOnRestore,
    fragmentTransactionConfigurator = fragmentTransactionConfigurator,
    fragmentsClassesFactory = fragmentsClassesFactory
)
