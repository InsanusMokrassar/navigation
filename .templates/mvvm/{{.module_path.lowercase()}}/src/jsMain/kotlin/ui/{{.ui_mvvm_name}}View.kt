package {{.full_module_package}}.ui


import androidx.compose.runtime.Composable
import dev.inmo.navigation.mvvm.View
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class {{.ui_mvvm_name}}View(
    config: {{.ui_mvvm_name}}ViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>
) : View<{{.ui_mvvm_name}}ViewConfig, {{.ui_mvvm_name}}ViewModel>(config, chain) {
    override val viewModel: {{.ui_mvvm_name}}ViewModel by inject {
        parametersOf(this)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
    }
}
