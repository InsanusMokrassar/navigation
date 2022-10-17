package dev.inmo.navigation.core.fragments

import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig

abstract class NodeFragment<Config : NavigationNodeDefaultConfig> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config>
        private set

    internal fun setNode(node: AndroidFragmentNode<Config>) {
        this.node = node
    }
}
