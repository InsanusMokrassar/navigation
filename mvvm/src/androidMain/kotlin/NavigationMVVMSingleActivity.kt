package dev.inmo.navigation.mvvm

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.navigationTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent

abstract class NavigationMVVMSingleActivity : AppCompatActivity(), KoinComponent {
    protected open val fragmentTransactionConfigurator: FragmentTransactionConfigurator<NavigationNodeDefaultConfig>? = null
    protected open fun allocateLoadingFragment(): Fragment? = null
    protected abstract fun createInitialConfig(rootFragmentTag: String): NavigationNodeDefaultConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootFragmentTag = findViewById<View>(R.id.main_activity_main_fragment).navigationTag.toString()

        val loadingFragment = allocateLoadingFragment()
        if (loadingFragment != null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.main_activity_main_fragment, loadingFragment)
            }.commitNow()
        }

        CoroutineScope(Dispatchers.Default).launchSafelyWithoutExceptions {
            onStartNavigation(rootFragmentTag)
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }

    open suspend fun onStartNavigation(rootFragmentTag: String) {
        doInUI {
            initNavigation<NavigationNodeDefaultConfig>(
                createInitialConfig(rootFragmentTag),
                getKoin().getAllDistinct<NavigationFragmentInfoProvider>(),
                manualHierarchyCheckerDelayMillis = 20L,
                fragmentTransactionConfigurator = fragmentTransactionConfigurator,
                dropRedundantChainsOnRestore = true
            )
        }
    }
}
