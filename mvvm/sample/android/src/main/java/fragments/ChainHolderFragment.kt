package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig

abstract class ChainHolderFragment<T : AndroidNodeConfig> : Fragment() {
    protected val chain = NavigationChain<AndroidNodeConfig>()
}
