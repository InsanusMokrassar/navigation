package dev.inmo.navigation.core.repo

import kotlinx.serialization.Serializable

@Serializable
sealed interface ConfigHolder<T> {
    @Serializable
    data class Node<T>(
        val config: T,
        val subnode: Node<T>?,
        val subchains: List<Chain<T>>
    ) : ConfigHolder<T>
    @Serializable
    data class Chain<T>(
        val firstNodeConfig: Node<T>
    ) : ConfigHolder<T>
}
