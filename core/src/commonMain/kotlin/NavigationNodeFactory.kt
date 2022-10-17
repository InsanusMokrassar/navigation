package dev.inmo.navigation.core

fun interface NavigationNodeFactory<T> {
    fun createNode(
        navigationChain: NavigationChain<T>,
        config: T
    ): NavigationNode<T>?
}
