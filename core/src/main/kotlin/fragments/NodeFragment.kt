package dev.inmo.navigation.core.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.flow.MutableStateFlow

abstract class NodeFragment<Config : Base, Base : NavigationNodeDefaultConfig> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config, Base>
        private set
    protected lateinit var _configState: MutableStateFlow<Config>
    protected var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }
    internal var configId: String
        get() = runCatching { config.id }.getOrElse { arguments ?.getString("configId") ?: throw it }
        set(value) {
            (arguments ?: Bundle().apply { arguments = this }).putString("configId", value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState ?.let {
            arguments = Bundle().apply {
                putAll(it)
                putAll(arguments ?: return@apply)
            }
        }
        configId = arguments ?.getString("configId") ?: config.id
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("configId", configId)
    }

    internal fun setNode(
        node: AndroidFragmentNode<Config, Base>,
        configStateFlow: MutableStateFlow<Config>
    ) {
        this.node = node
        this._configState = configStateFlow
        configId = configStateFlow.value.id
    }
}
