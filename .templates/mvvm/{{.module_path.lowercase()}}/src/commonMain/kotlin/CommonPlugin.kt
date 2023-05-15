package {{.full_module_package}}

import {{.full_module_package}}.ui.{{.ui_mvvm_name}}Model
import {{.full_module_package}}.ui.{{.ui_mvvm_name}}ViewConfig
import {{.full_module_package}}.ui.{{.ui_mvvm_name}}ViewModel
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

object CommonPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {

        factory { (node: NavigationNode<{{.ui_mvvm_name}}ViewConfig, NavigationNodeDefaultConfig>) ->
            {{.ui_mvvm_name}}ViewModel(get(), node)
        }

        single<{{.ui_mvvm_name}}Model> {
            object : {{.ui_mvvm_name}}Model {

            }
        }

    }
}
