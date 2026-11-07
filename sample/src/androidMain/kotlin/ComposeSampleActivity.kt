package dev.inmo.navigation.sample

import androidx.lifecycle.lifecycleScope
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.navigation.compose.NavigationComposeSingleActivity
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import kotlinx.coroutines.async
import org.koin.core.context.stopKoin
import kotlin.reflect.KClass

class ComposeSampleActivity : NavigationComposeSingleActivity<NavigationNodeDefaultConfig>() {
    private val startup by lazy {
        lifecycleScope.async {
            stopKoin()
            StartLauncherPlugin.start(Config(listOf(AndroidPlugin(this@ComposeSampleActivity))))
        }
    }

    override val baseClassName: KClass<NavigationNodeDefaultConfig>
        get() = NavigationNodeDefaultConfig::class

    override fun createInitialConfigChain(): ConfigHolder.Chain<NavigationNodeDefaultConfig> {
        return ConfigHolder.Chain(
            ConfigHolder.Node(
                NavigationViewConfig("", "Node"),
                null,
                emptyList()
            )
        )
    }

    override suspend fun onBeforeStartNavigation() {
        startup.await()
    }
}
