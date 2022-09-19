package dev.inmo.navigation.core

fun interface NavigationNodeFactory<T> {
    fun createNode(
        chainHolder: NavigationChain<T>,
        config: T
    ): NavigationNode<T>?
}
