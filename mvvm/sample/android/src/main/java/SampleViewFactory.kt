package dev.inmo.navigation.mvvm.sample.android

import dev.inmo.navigation.core.*

class SampleViewFactory : NavigationNodeFactory<AndroidNodeConfig> {
    override fun createNode(chainHolder: NavigationChain<AndroidNodeConfig>, config: AndroidNodeConfig): NavigationNode<AndroidNodeConfig>? {
        return when (config) {
            is AndroidNodeConfig.TextConfig -> TODO()
            else -> null
        }
    }
}
