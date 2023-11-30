package dev.inmo.navigation.sample.ui


import androidx.compose.runtime.Composable
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.View
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class NavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>
) : View<NavigationViewConfig, NavigationViewModel>(config, chain) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    override fun onCreate() {
        super.onCreate()
        viewModel // just init viewModel
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
    }
}
