package dev.inmo.navigation.compose

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode

internal data class ComposeInjectedChainsAndNodesIds(
    val chains: MutableSet<NavigationChain<*>> = mutableSetOf(),
    val nodes: MutableSet<NavigationNode<*, *>> = mutableSetOf(),
)
