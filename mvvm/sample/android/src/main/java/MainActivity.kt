package dev.inmo.navigation.mvvm.sample.android

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.common.rootView
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.fragments.AndroidFragmentNode
import dev.inmo.navigation.core.repo.*
import dev.inmo.navigation.mvvm.sample.android.fragments.TextFragment
import dev.inmo.navigation.core.AndroidSPConfigsRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootFragmentTag = findViewById<View>(R.id.fragment_id).tag.toString()

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val repo = AndroidSPConfigsRepo(
            getSharedPreferences("internal", MODE_PRIVATE),
            AndroidNodeConfig::class
        )

        lateinit var rootChainHolder: NavigationChain<AndroidNodeConfig>
        val factory = NavigationNodeFactory<AndroidNodeConfig> { navigationChain, config ->
            when (config) {
                is AndroidNodeConfig.TextConfig -> AndroidFragmentNode(
                    navigationChain,
                    config,
                    config.viewTag,
                    TextFragment::class,
                    supportFragmentManager,
                    rootView!!
                )

                else -> error(config)
            }.also {
                it.chain.enableSavingHierarchy(
                    repo,
                    scope,
                    1000L
                )
            }
        }
        scope.launch {
            rootChainHolder = restoreHierarchy<AndroidNodeConfig>(
                repo.get() ?: ConfigHolder.Chain(
                    ConfigHolder.Node(
                        AndroidNodeConfig.TextConfig(
                            rootFragmentTag,
                            "Sample"
                        ),
                        null,
                        emptyList()
                    )
                ),
                scope,
                factory = factory
            ) ?: return@launch
        }
    }
}
