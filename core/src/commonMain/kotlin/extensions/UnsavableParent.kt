package dev.inmo.navigation.core.extensions

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig

fun NavigationNode<*, *>.stackStorableInNavigationHierarchy(): Boolean {
    val stack = chain.stack
    val index = stack.indexOf(this).takeIf { it > -1 } ?: return false

    return storableInNavigationHierarchy && (index == 0 || stack[index - 1].stackStorableInNavigationHierarchy())
}
fun NavigationNode<*, *>.recursivelyStorableInNavigationHierarchy(): Boolean {
    return storableInNavigationHierarchy && chain.recursivelyStorableInNavigationHierarchy()
}
fun NavigationChain<*>.recursivelyStorableInNavigationHierarchy() = parentNode ?.recursivelyStorableInNavigationHierarchy() ?: true
fun NavigationChain<*>.stackStorableInNavigationHierarchy() = stack.lastOrNull() ?.stackStorableInNavigationHierarchy() ?: false