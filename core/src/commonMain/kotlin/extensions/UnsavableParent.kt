package dev.inmo.navigation.core.extensions

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig

fun NavigationNode<*, *>.recursivelyStorableInNavigationHierarchy(): Boolean {
    val config = config

    return when (config) {
        is NavigationNodeDefaultConfig -> config.storableInNavigationHierarchy
        else -> true
    } && chain.recursivelyStorableInNavigationHierarchy()
}
fun NavigationChain<*>.recursivelyStorableInNavigationHierarchy() = parentNode ?.recursivelyStorableInNavigationHierarchy() ?: true