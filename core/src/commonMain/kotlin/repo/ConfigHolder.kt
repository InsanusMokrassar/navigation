package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.NavigationChainId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ConfigHolder<T> {
    @Serializable
    @SerialName("Node")
    data class Node<T>(
        val config: T,
        val subnode: Node<T>?,
        val subchains: List<Chain<T>>
    ) : ConfigHolder<T>
    @Serializable
    @SerialName("Chain")
    data class Chain<T>(
        val firstNodeConfig: Node<T>,
        val id: NavigationChainId? = null
    ) : ConfigHolder<T>
}
