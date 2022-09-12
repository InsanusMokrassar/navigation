package dev.inmo.navigation.core

fun interface NavigationNodeFactory<T> {
    fun createNode(
        chainHolder: ChainHolder<T>,
        config: T
    ): NavigationNode<T>?
}
