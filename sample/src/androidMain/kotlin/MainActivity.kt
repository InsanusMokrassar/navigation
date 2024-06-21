package dev.inmo.navigation.sample

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.fragment.app.Fragment
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.NavigationComposeSingleActivity
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.mvvm.NavigationMVVMSingleActivity
import dev.inmo.navigation.sample.ui.LoadingFragment
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import kotlin.reflect.KClass

class MainActivity : NavigationComposeSingleActivity<NavigationNodeDefaultConfig>() {
    private class Plugin(private val mainActivity: MainActivity) : StartPlugin {
        override fun Module.setupDI(config: JsonObject) {
            single { mainActivity }
            single<Context> { mainActivity }
            single<Resources> { mainActivity.resources }
            single<Application> { mainActivity.application }
        }
    }

//    override val fragmentTransactionConfigurator: FragmentTransactionConfigurator<NavigationNodeDefaultConfig>? = FragmentTransactionConfigurator.Default {
//        setCustomAnimations(
//            /* enter = */ R.anim.slide_in,
//            /* exit = */ R.anim.fade_out,
//            /* popEnter = */ R.anim.fade_in,
//            /* popExit = */ R.anim.slide_out,
//        )
//    }
    private val plugins: List<StartPlugin> by lazy {
        listOf(
            Plugin(this),
            AndroidPlugin
        )
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
        stopKoin()
        StartLauncherPlugin.start(
            Config(
                plugins
            )
        )

        super.onBeforeStartNavigation()
    }
}
