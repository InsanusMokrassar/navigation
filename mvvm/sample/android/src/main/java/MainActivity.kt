package dev.inmo.navigation.mvvm.sample.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.fragments.AndroidFragmentNode
import dev.inmo.navigation.core.repo.storeHierarchy
import dev.inmo.navigation.mvvm.sample.android.fragments.TextFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        lateinit var rootChainHolder: NavigationChain<AndroidNodeConfig>
        rootChainHolder = NavigationChain<AndroidNodeConfig>(
            null,
            scope,
            NavigationNodeFactory { navigationChain, config ->
                when (config) {
                    is AndroidNodeConfig.TextConfig -> AndroidFragmentNode(
                        navigationChain,
                        config,
                        config.viewId,
                        TextFragment::class,
                        supportFragmentManager
                    )

                    else -> error(config)
                }.also {
                    it.statesFlow.filter {
                        it == NavigationNodeState.RESUMED || it == NavigationNodeState.CREATED
                    }.subscribeSafelyWithoutExceptions(scope + it.chain.job) {
                        val textView = findViewById<TextView>(R.id.activity_main_tree_text)
                        textView.text = rootChainHolder.storeHierarchy().toString()
                    }
                    it.subchainsFlow.subscribeSafelyWithoutExceptions(scope + it.chain.job) {
                        val textView = findViewById<TextView>(R.id.activity_main_tree_text)
                        textView.text = rootChainHolder.storeHierarchy().toString()
                    }
                }
            }
        )

        rootChainHolder.push(
            AndroidNodeConfig.TextConfig(
                R.id.fragment_id,
                "Sample"
            )
        )
    }
}
