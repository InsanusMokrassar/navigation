package dev.inmo.navigation.sample

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewFragment
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import kotlin.reflect.KClass

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationFragmentInfoProvider> {
            object : NavigationFragmentInfoProvider {
                override val configsKClasses: Set<KClass<*>> = setOf(
                    NavigationViewConfig::class,
                )

                override fun resolveFragmentKClass(config: NavigationNodeDefaultConfig): KClass<*>? {
                    return when (config) {
                        is NavigationViewConfig -> NavigationViewFragment::class
                        else -> null
                    }
                }
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonPlugin.startPlugin(koin)
    }
}
