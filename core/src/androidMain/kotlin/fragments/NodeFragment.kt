package dev.inmo.navigation.core.fragments

import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.flow.MutableStateFlow

abstract class NodeFragment<Config : Base, Base : NavigationNodeDefaultConfig> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config, Base>
        private set
    protected lateinit var _configState: MutableStateFlow<Config>
    protected var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }

    internal fun setNode(
        node: AndroidFragmentNode<Config, Base>,
        configStateFlow: MutableStateFlow<Config>
    ) {
        this.node = node
        this._configState = configStateFlow
    }
}
