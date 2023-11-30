package dev.inmo.navigation.sample

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.sample.ui.NavigationView
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.CookiesNavigationConfigsRepo
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.initNavigation
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.OneParameterUrlNavigationConfigsRepo
import kotlinx.browser.window
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import org.w3c.dom.url.URLSearchParams

object JSPlugin : StartPlugin {
    private val Koin.nodeFactory: NavigationNodeFactory<NavigationNodeDefaultConfig>
        get() {
            val factories = getAllDistinct<NavigationNodeFactory<NavigationNodeDefaultConfig>>()
            return NavigationNodeFactory<NavigationNodeDefaultConfig> { chainHolder, config ->
                factories.firstNotNullOfOrNull { it.createNode(chainHolder, config) }
            }.also {
                this@JSPlugin.logger.d { "Navigation node factory inited with next factories: $factories" }
            }
        }
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

        initNavigation<NavigationNodeDefaultConfig>(
            ConfigHolder.Chain(
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
            dropRedundantChainsOnRestore = true,
            nodesFactory = koin.nodeFactory,
        )
    }
}
