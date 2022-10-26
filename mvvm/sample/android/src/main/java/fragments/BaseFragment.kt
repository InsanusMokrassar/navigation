package dev.inmo.navigation.mvvm.sample.android.fragments

import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.SampleConfig

abstract class BaseFragment<Config : SampleConfig> : NodeFragment<Config, SampleConfig>() {
}
