package dev.inmo.navigation.mvvm.sample.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.subject.SubjectNavigationNode
import dev.inmo.navigation.mvvm.sample.android.fragments.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootChainHolder = NavigationChain<AndroidNodeConfig>(
            null,
            CoroutineScope(Dispatchers.Main),
            NavigationNodeFactory { navigationChain, config ->
                when (config) {
                    is AndroidNodeConfig.TextConfig -> FragmentSubjectNode(
                        navigationChain,
                        TextFragment::class,
                        config
                    )

                    else -> error(config)
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
