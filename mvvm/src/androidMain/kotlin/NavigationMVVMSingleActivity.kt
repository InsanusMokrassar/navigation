package dev.inmo.navigation.mvvm

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.contextaware.OnContextAvailableListener
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.AndroidNavigationHost
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import dev.inmo.navigation.core.navigationTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent

abstract class NavigationMVVMSingleActivity : AppCompatActivity(), KoinComponent {
    protected open val fragmentTransactionConfigurator: FragmentTransactionConfigurator<NavigationNodeDefaultConfig>? = null
    protected var currentAndroidNavigationHost: AndroidNavigationHost<NavigationNodeDefaultConfig>? = null
    protected var latestLoadingFragment: Fragment? = null
    protected open fun allocateLoadingFragment(): Fragment? = null
    protected abstract fun createInitialConfig(rootFragmentTag: String): NavigationNodeDefaultConfig

    protected fun initNavigation() {
        val rootFragmentTag = findViewById<View>(R.id.main_activity_main_fragment).navigationTag.toString()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_activity_main_fragment)

        if (currentFragment === latestLoadingFragment || currentFragment == null) {
            CoroutineScope(Dispatchers.Default).launchSafelyWithoutExceptions {
                onStartNavigation(rootFragmentTag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ConfigHolder restores navigation nodes and binds fresh Fragment instances.
        // FragmentActivity's earlier context listener restores its own instances;
        // discard those before dispatchCreate can call an unbound node's lifecycle.
        val restoreListener = savedInstanceState?.let {
            OnContextAvailableListener {
                val restoredNodes = supportFragmentManager.fragments.filterIsInstance<NodeFragment<*, *>>()
                if (restoredNodes.isNotEmpty()) {
                    supportFragmentManager.beginTransaction().apply {
                        setReorderingAllowed(true)
                        restoredNodes.forEach { remove(it) }
                    }.commitNow()
                }
            }
        }
        restoreListener?.let(::addOnContextAvailableListener)
        try {
            super.onCreate(savedInstanceState)
        } finally {
            restoreListener?.let(::removeOnContextAvailableListener)
        }
        setContentView(R.layout.activity_main)

        val loadingFragment = allocateLoadingFragment()
        if (loadingFragment != null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.main_activity_main_fragment, loadingFragment)
            }.commitNow()
            latestLoadingFragment = loadingFragment
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        initNavigation()
    }

    open suspend fun onStartNavigation(rootFragmentTag: String) {
        doInUI {
            currentAndroidNavigationHost ?.job ?.cancel()
            currentAndroidNavigationHost = initNavigation<NavigationNodeDefaultConfig>(
                createInitialConfig(rootFragmentTag),
                getKoin().getAllDistinct<NavigationFragmentInfoProvider>(),
                manualHierarchyCheckerDelayMillis = 20L,
                fragmentTransactionConfigurator = fragmentTransactionConfigurator,
                dropRedundantChainsOnRestore = true
            )
        }
    }
}
