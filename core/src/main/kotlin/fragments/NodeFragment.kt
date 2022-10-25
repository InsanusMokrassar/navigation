package dev.inmo.navigation.core.fragments

import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.flow.MutableStateFlow

abstract class NodeFragment<Config : NavigationNodeDefaultConfig> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config>
        private set
    protected lateinit var _configState: MutableStateFlow<Config>

    internal fun setNode(
        node: AndroidFragmentNode<Config>,
        configStateFlow: MutableStateFlow<Config>
    ) {
        this.node = node
        this._configState = configStateFlow
    }
}
