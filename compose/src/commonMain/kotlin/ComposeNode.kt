package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.SpecialMutableStateFlow
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.JvmName

/**
 * Provides [onDraw] open function which will be called by the navigation system to draw content in the place it added
 */
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
        drawerState.value = @Composable { onDraw() }
    }

    override fun onPause() {
        super.onPause()
        drawerState.value = null
    }

    @Composable
    protected open fun onDraw() {}

    /**
     * Provides place for [NavigationChain] which placed in [subchainsFlow]
     */
    @Composable
    protected open fun SubchainsHost(filter: (NavigationChain<Base>) -> Boolean) {
        val subchainsState = subchainsFlow.collectAsState()
        val rawSubchains = subchainsState.value
        val filteredSubchains = rawSubchains.filter(filter)
        filteredSubchains.forEach {
            it.StartInCompose()
        }
    }

    @Composable
    protected fun SubchainsHost() = SubchainsHost { true }
}