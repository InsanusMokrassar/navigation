package {{.full_module_package}}

import {{.full_module_package}}.ui.{{.ui_mvvm_name}}View
import {{.full_module_package}}.ui.{{.ui_mvvm_name}}ViewConfig
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<NavigationNodeDefaultConfig>> {
            NavigationNodeFactory { chain: NavigationChain<NavigationNodeDefaultConfig>, config: NavigationNodeDefaultConfig ->
                if (config is {{.ui_mvvm_name}}ViewConfig) {
                    {{.ui_mvvm_name}}View(config, chain)
                } else {
                    null
                }
            }
        }

        singleWithRandomQualifier<SerializersModule> {
            SerializersModule {
                polymorphic(Any::class, {{.ui_mvvm_name}}ViewConfig::class, {{.ui_mvvm_name}}ViewConfig.serializer())
                polymorphic(NavigationNodeDefaultConfig::class, {{.ui_mvvm_name}}ViewConfig::class, {{.ui_mvvm_name}}ViewConfig.serializer())
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonPlugin.startPlugin(koin)
    }
}
