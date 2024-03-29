package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.coroutines.FlowOnHierarchyChangeListener
import dev.inmo.micro_utils.coroutines.setOnHierarchyChangeListenerRecursively
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator

class AndroidNavigationNodeFactory<T : NavigationNodeDefaultConfig>(
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener = FlowOnHierarchyChangeListener(recursive = true).also {
        (rootView as ViewGroup).setOnHierarchyChangeListenerRecursively(it)
    },
    private val manualHierarchyCheckerDelayMillis: Long? = 100L,
    private val fragmentTransactionConfigurator: FragmentTransactionConfigurator<T>? = null,
    private val fragmentKClassResolver: FragmentsClassesFactory<T>
) : NavigationNodeFactory<T> {
    override fun createNode(navigationChain: NavigationChain<T>, config: T): NavigationNode<out T, T>? {
        return AndroidFragmentNode(
            navigationChain,
            config,
            fragmentKClassResolver(config) ?: return null,
            fragmentManager,
            rootView,
            flowOnHierarchyChangeListener,
            manualHierarchyCheckerDelayMillis,
            fragmentTransactionConfigurator = fragmentTransactionConfigurator
        )
    }
}
