package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.navigation.sample.ui.NavigationModel
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.sample.ui.NavigationView
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

object CommonPlugin : StartPlugin {
    var useDefaultNavigationViewFactory = true
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

        if (useDefaultNavigationViewFactory) {
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
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)

        koin.get<(@Composable () -> Unit) -> Unit>().invoke {
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
