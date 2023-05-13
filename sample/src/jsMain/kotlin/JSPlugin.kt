package dev.inmo.navigation.sample

import dev.inmo.navigation.sample.ui.NavigationView
import dev.inmo.navigation.sample.ui.NavigationViewConfig
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
                if (config is NavigationViewConfig) {
                    NavigationView(config, chain)
                } else {
                    null
                }
            }
        }

        singleWithRandomQualifier<SerializersModule> {
            SerializersModule {
                polymorphic(Any::class, NavigationViewConfig::class, NavigationViewConfig.serializer())
                polymorphic(NavigationNodeDefaultConfig::class, NavigationViewConfig::class, NavigationViewConfig.serializer())
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonPlugin.startPlugin(koin)
    }
}
