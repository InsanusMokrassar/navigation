package dev.inmo.navigation.sample.android.fragments

import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.sample.android.SampleConfig

abstract class BaseFragment<Config : SampleConfig> : NodeFragment<Config, SampleConfig>() {
}
