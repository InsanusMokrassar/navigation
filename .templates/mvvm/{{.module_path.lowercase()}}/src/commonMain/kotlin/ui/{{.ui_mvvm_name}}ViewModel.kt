package {{.full_module_package}}.ui

import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig

class {{.ui_mvvm_name}}ViewModel (
    private val model: {{.ui_mvvm_name}}Model,
    private val node: NavigationNode<{{.ui_mvvm_name}}ViewConfig, NavigationNodeDefaultConfig>,
) : ViewModel(node) {

}
