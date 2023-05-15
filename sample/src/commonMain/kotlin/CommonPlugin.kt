package dev.inmo.navigation.sample

import dev.inmo.navigation.sample.ui.NavigationModel
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.json.JsonObject
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

    }
}
