package dev.inmo.navigation.sample

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationView
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import kotlin.reflect.KClass

object AndroidPlugin : StartPlugin {
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
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonPlugin.startPlugin(koin)


    }
}
