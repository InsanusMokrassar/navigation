package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.SpecialMutableStateFlow
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    val currentDrawingBlock = SpecialMutableStateFlow<@Composable () -> Unit>({})
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        single {
            { drawable: @Composable () -> Unit ->
                currentDrawingBlock.value = drawable
            }
        }

        single<NavigationConfigsRepo<NavigationNodeDefaultConfig>> {
            NavigationConfigsRepo.InMemory()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonPlugin.startPlugin(koin)
    }
}
