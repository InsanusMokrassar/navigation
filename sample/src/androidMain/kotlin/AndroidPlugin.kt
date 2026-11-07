package dev.inmo.navigation.sample

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewFragment
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module
import kotlin.reflect.KClass

class AndroidPlugin(private val activity: AppCompatActivity) : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (CommonPlugin) { setupDI(config) }

        single { activity }
        single<Context> { activity }
        single<Resources> { activity.resources }
        single<Application> { activity.application }

        singleWithRandomQualifier<NavigationFragmentInfoProvider> {
            object : NavigationFragmentInfoProvider {
                override val configsKClasses: Set<KClass<*>> = setOf(NavigationViewConfig::class)

                override fun resolveFragmentKClass(config: NavigationNodeDefaultConfig): KClass<*>? =
                    when (config) {
                        is NavigationViewConfig -> NavigationViewFragment::class
                        else -> null
                    }
            }
        }
    }
}
