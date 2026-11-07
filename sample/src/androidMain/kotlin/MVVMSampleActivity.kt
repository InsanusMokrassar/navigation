package dev.inmo.navigation.sample

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.navigation.core.AndroidSPConfigsRepo
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.mvvm.NavigationFragmentInfoProvider
import dev.inmo.navigation.mvvm.NavigationMVVMSingleActivity
import dev.inmo.navigation.mvvm.initNavigation
import dev.inmo.navigation.sample.ui.LoadingFragment
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import kotlinx.coroutines.async
import org.koin.core.context.stopKoin

class MVVMSampleActivity : NavigationMVVMSingleActivity() {
    private val startup by lazy {
        lifecycleScope.async {
            stopKoin()
            StartLauncherPlugin.start(Config(listOf(AndroidPlugin(this@MVVMSampleActivity))))
        }
    }

    override val fragmentTransactionConfigurator = FragmentTransactionConfigurator.Default<NavigationNodeDefaultConfig> {
        setCustomAnimations(
            R.anim.slide_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.slide_out
        )
    }

    override fun createInitialConfig(rootFragmentTag: String): NavigationNodeDefaultConfig =
        NavigationViewConfig(rootFragmentTag, "Node")

    override fun allocateLoadingFragment(): Fragment = LoadingFragment()

    override suspend fun onStartNavigation(rootFragmentTag: String) {
        startup.await()

        doInUI {
            if (isFinishing || isDestroyed) {
                return@doInUI
            }
            currentAndroidNavigationHost?.job?.cancel()
            currentAndroidNavigationHost = initNavigation<NavigationNodeDefaultConfig>(
                startConfig = createInitialConfig(rootFragmentTag),
                navigationFragmentInfoProviders = getKoin().getAllDistinct<NavigationFragmentInfoProvider>(),
                configsRepo = AndroidSPConfigsRepo(
                    getSharedPreferences("view_fragment_sample", MODE_PRIVATE),
                    NavigationNodeDefaultConfig::class,
                    json = getKoin().get()
                ),
                scope = lifecycleScope,
                manualHierarchyCheckerDelayMillis = 20L,
                fragmentTransactionConfigurator = fragmentTransactionConfigurator,
                dropRedundantChainsOnRestore = true
            )
        }
    }
}
