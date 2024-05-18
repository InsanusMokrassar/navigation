package dev.inmo.navigation.sample

import dev.inmo.navigation.sample.ui.NavigationView
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.OneParameterUrlNavigationConfigsRepo
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<NavigationNodeDefaultConfig>> {
            OneParameterUrlNavigationConfigsRepo(
                get(),
                PolymorphicSerializer(NavigationNodeDefaultConfig::class)
            )
        }

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

        renderComposable("root") {
            dev.inmo.navigation.compose.initNavigation(
                defaultStartChain = ConfigHolder.Chain(
                    ConfigHolder.Node(
                        NavigationViewConfig(
                            "root",
                            ">"
                        ),
                        null,
                        listOf()
                    ),
                ),
                configsRepo = koin.get(),
                nodesFactory = koin.nodeFactory(),
                dropRedundantChainsOnRestore = true,
            )
        }
    }
}
