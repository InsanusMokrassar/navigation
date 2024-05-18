package dev.inmo.navigation.sample

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.navigation.sample.ui.NavigationModel
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

object CommonPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {

        factory { (node: NavigationNode<NavigationViewConfig, NavigationNodeDefaultConfig>) ->
            NavigationViewModel(get(), node)
        }

        single<NavigationModel> {
            object : NavigationModel {

            }
        }

        singleWithRandomQualifier<SerializersModule> {
            SerializersModule {
                polymorphic(Any::class, NavigationViewConfig::class, NavigationViewConfig.serializer())
                polymorphic(NavigationNodeDefaultConfig::class, NavigationViewConfig::class, NavigationViewConfig.serializer())
            }
        }

    }
}
