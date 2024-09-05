package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.inmo.micro_utils.coroutines.SpecialMutableStateFlow
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.OneParameterUrlNavigationConfigsRepo
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    val currentDrawingBlock = SpecialMutableStateFlow<@Composable () -> Unit>({})
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<NavigationNodeDefaultConfig>> {
            OneParameterUrlNavigationConfigsRepo(
                get(),
                PolymorphicSerializer(NavigationNodeDefaultConfig::class)
            )
        }

        single {
            { drawable: @Composable () -> Unit ->
                currentDrawingBlock.value = drawable
            }
        }

        singleWithRandomQualifier<NavigationNodeFactory<NavigationNodeDefaultConfig>> {
            NavigationNodeFactory { chain: NavigationChain<NavigationNodeDefaultConfig>, config: NavigationNodeDefaultConfig ->
                if (config is NavigationViewConfig) {
                    JSNavigationView(config, chain)
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
            currentDrawingBlock.collectAsState().value.invoke()
        }
    }
}
