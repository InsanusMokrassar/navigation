package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.SpecialMutableStateFlow
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class ComposeNode<Config : Base, Base>(
    config: Config,
    override val chain: NavigationChain<Base>,
    id: NavigationNodeId = NavigationNodeId()
) : NavigationNode<Config, Base>(id) {
    protected val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
    override var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }
    internal val drawerState: SpecialMutableStateFlow<(@Composable () -> Unit)?> = SpecialMutableStateFlow(null)

    override fun onResume() {
        super.onResume()
        drawerState.value = this::onDraw
    }

    override fun onPause() {
        super.onPause()
        drawerState.value = null
    }

    @Composable
    protected open fun onDraw() {}
}