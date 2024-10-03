package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.navigation.sample.ui.NavigationModel
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.sample.ui.NavigationView
import dev.inmo.navigation.sample.ui.tree.CurrentTreeViewConfig
import dev.inmo.navigation.sample.ui.tree.CurrentTreeViewViewModel
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
        singleWithRandomQualifier<NavigationNodeFactory<NavigationNodeDefaultConfig>> {
            NavigationNodeFactory { chain: NavigationChain<NavigationNodeDefaultConfig>, config: NavigationNodeDefaultConfig ->
                if (config is EmptyConfig) {
                    NavigationNode.Empty(chain, config)
                } else {
                    null
                }
            }
        }

        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, CurrentTreeViewConfig::class, CurrentTreeViewConfig.serializer())
                polymorphic(NavigationNodeDefaultConfig::class, CurrentTreeViewConfig::class, CurrentTreeViewConfig.serializer())

                polymorphic(Any::class, EmptyConfig::class, EmptyConfig.serializer())
                polymorphic(NavigationNodeDefaultConfig::class, EmptyConfig::class, EmptyConfig.serializer())
            }
        }
        factory {
            CurrentTreeViewViewModel(it.get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)

        koin.get<(@Composable () -> Unit) -> Unit>().invoke {
            dev.inmo.navigation.compose.initNavigation<NavigationNodeDefaultConfig>(
                EmptyConfig(""),
                configsRepo = koin.get(),
                nodesFactory = koin.nodeFactory(),
                dropRedundantChainsOnRestore = true,
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    InjectNavigationNode(
                        NavigationViewConfig(id = "root", text = ">")
                    )
                }
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    InjectNavigationNode(CurrentTreeViewConfig(id = "tree"))
                }
            }
        }
    }
}
