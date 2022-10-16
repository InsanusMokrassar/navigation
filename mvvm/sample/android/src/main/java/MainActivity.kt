package dev.inmo.navigation.mvvm.sample.android

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.repo.*
import dev.inmo.navigation.mvvm.sample.android.fragments.TextFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootFragmentTag = findViewById<View>(R.id.fragment_id).navigationTag.toString()

        initNavigation<SampleConfig>(
            ConfigHolder.Chain(
                ConfigHolder.Node(
                    SampleConfig.TextConfig(
                        rootFragmentTag,
                        getString(R.string.forward)
                    ),
                    null,
                    emptyList()
                )
            )
        ) { config ->
            when (config) {
                is SampleConfig.TextConfig -> TextFragment::class
            }
        }
    }
}
