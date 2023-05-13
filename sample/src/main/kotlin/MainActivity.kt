package dev.inmo.navigation.sample

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.AndroidSPConfigsRepo
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.initNavigation
import dev.inmo.navigation.core.navigationTag
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.sample.ui.LoadingFragment
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

class MainActivity : AppCompatActivity() {
    private class Plugin(private val mainActivity: MainActivity) : StartPlugin {
        override fun Module.setupDI(config: JsonObject) {
            single { mainActivity }
            single<Context> { mainActivity }
            single<Application> { mainActivity.application }
        }
    }
    private val plugins: List<StartPlugin> by lazy {
        listOf(
            Plugin(this),
            AndroidPlugin
        )
    }

    init {
        activity = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootFragmentTag = findViewById<View>(R.id.main_activity_main_fragment).navigationTag.toString()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.main_activity_main_fragment, LoadingFragment())
        }.commitNow()

        CoroutineScope(Dispatchers.Default).launchSafelyWithoutExceptions {
            stopKoin()
            StartLauncherPlugin.start(
                Config(
                    plugins
                )
            )
            doInUI {
                initNavigation<NavigationNodeDefaultConfig>(
                    NavigationViewConfig(rootFragmentTag, "Node"),
                    AndroidSPConfigsRepo(
                        getSharedPreferences("internal", MODE_PRIVATE),
                        NavigationNodeDefaultConfig::class,
                        NavigationViewConfig::class,
                    ),
                    scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
                    manualHierarchyCheckerDelayMillis = 20L,
                    fragmentsClassesFactory = {
                        when (it) {
                            is NavigationViewConfig -> NavigationViewFragment::class
                            else -> null
                        }
                    },
                    dropRedundantChainsOnRestore = true
                )
            }
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }


    companion object {
        lateinit var activity: MainActivity
            private set
    }
}
