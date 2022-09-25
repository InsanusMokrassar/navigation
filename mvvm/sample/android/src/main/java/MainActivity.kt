package dev.inmo.navigation.mvvm.sample.android

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.common.rootView
import dev.inmo.micro_utils.coroutines.FlowOnHierarchyChangeListener
import dev.inmo.micro_utils.coroutines.setOnHierarchyChangeListenerRecursively
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.fragments.AndroidFragmentNode
import dev.inmo.navigation.core.repo.*
import dev.inmo.navigation.mvvm.sample.android.fragments.TextFragment
import dev.inmo.navigation.core.AndroidSPConfigsRepo
import dev.inmo.navigation.core.fragments.AndroidNavigationNodeFactory
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootFragmentTag = findViewById<View>(R.id.fragment_id).tag.toString()

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val repo = AndroidSPConfigsRepo(
            getSharedPreferences("internal", MODE_PRIVATE),
            SampleConfig::class
        )

        val surrogateFactory = AndroidNavigationNodeFactory<SampleConfig>(
            supportFragmentManager,
            rootView!!
        ) { config ->
            when (config) {
                is SampleConfig.TextConfig -> TextFragment::class
            }
        }
        val factory = NavigationNodeFactory { navigationChain, config ->
            surrogateFactory.createNode(navigationChain, config) ?.also {
                it.chain.enableSavingHierarchy(
                    repo,
                    scope,
                    debounce = 1000L
                )
            }
        }

        scope.launch {
            restoreHierarchy<SampleConfig>(
                repo.get() ?: ConfigHolder.Chain(
                    ConfigHolder.Node(
                        SampleConfig.TextConfig(
                            rootFragmentTag,
                            getString(R.string.forward)
                        ),
                        null,
                        emptyList()
                    )
                ),
                factory = factory
            ) ?.start(scope) ?: return@launch
        }
    }
}
