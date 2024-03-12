package dev.inmo.navigation.core.fragments.transactions

import androidx.fragment.app.FragmentTransaction
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.AndroidFragmentNode

interface FragmentTransactionConfigurator<T : NavigationNodeDefaultConfig> {
    fun FragmentTransaction.onPlace(node: AndroidFragmentNode<out T, T>)
    fun FragmentTransaction.onRemove(node: AndroidFragmentNode<out T, T>)

    fun interface Default<T : NavigationNodeDefaultConfig> : FragmentTransactionConfigurator<T> {
        operator fun FragmentTransaction.invoke(node: AndroidFragmentNode<out T, T>)
        override fun FragmentTransaction.onPlace(node: AndroidFragmentNode<out T, T>) = this.invoke(node)
        override fun FragmentTransaction.onRemove(node: AndroidFragmentNode<out T, T>) = this.invoke(node)
    }

    class Callbacks<T : NavigationNodeDefaultConfig>(
        private val onPlaceFun: FragmentTransaction.(AndroidFragmentNode<out T, T>) -> Unit,
        private val onRemoveFun: FragmentTransaction.(AndroidFragmentNode<out T, T>) -> Unit,
    ) : FragmentTransactionConfigurator<T> {
        override fun FragmentTransaction.onPlace(node: AndroidFragmentNode<out T, T>) {
            onPlaceFun(node)
        }

        override fun FragmentTransaction.onRemove(node: AndroidFragmentNode<out T, T>) {
            onRemoveFun(node)
        }
    }
}