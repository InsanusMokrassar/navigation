package dev.inmo.navigation.core

fun interface NavigationNodeFactory<Base> {
    fun createNode(
        navigationChain: NavigationChain<Base>,
        config: Base
    ): NavigationNode<out Base, Base>?
}
