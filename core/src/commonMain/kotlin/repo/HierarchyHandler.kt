package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

suspend fun <T> ConfigHolder.Chain<T>.restoreHierarchy(
    node: NavigationNode<T>
): NavigationChain<T> {
    val subchain = node.createEmptySubChain()

    firstNodeConfig.restoreHierarchy(subchain)

    return subchain
}

suspend fun <T> ConfigHolder.Chain<T>.restoreHierarchy(
    factory: NavigationNodeFactory<T>
): NavigationChain<T> {
    val subchain = NavigationChain(null, factory)

    firstNodeConfig.restoreHierarchy(subchain)

    return subchain
}

suspend fun <T> ConfigHolder.Node<T>.restoreHierarchy(
    chain: NavigationChain<T>,
): NavigationNode<T>? {
    val node = chain.push(config) ?: return null

    subchains.forEach {
        it.restoreHierarchy(node)
    }

    subnode ?.restoreHierarchy(chain)

    return node
}

suspend fun <T> NavigationChain<T>.restoreHierarchy(
    holder: ConfigHolder.Node<T>
): NavigationNode<T>? {
    return holder.restoreHierarchy(
        this
    )
}

suspend fun <T> NavigationNode<T>.restoreHierarchy(
    holder: ConfigHolder.Chain<T>,
): NavigationChain<T> {
    return holder.restoreHierarchy(
        this
    )
}

suspend fun <T> restoreHierarchy(
    holder: ConfigHolder<T>,
    factory: NavigationNodeFactory<T>
): NavigationChain<T>? {
    return when (holder) {
        is ConfigHolder.Chain -> {
            holder.restoreHierarchy(
                factory
            )
        }
        is ConfigHolder.Node -> {
            holder.restoreHierarchy(
                NavigationChain(null, factory)
            ) ?.chain
        }
    }
}

fun <T> NavigationChain<T>.storeHierarchy(): ConfigHolder.Chain<T>? {
    return ConfigHolder.Chain(
        stack.firstOrNull() ?.storeHierarchy() ?: return null // skip empty chains
    )
}

fun <T> NavigationNode<T>.storeHierarchy(): ConfigHolder.Node<T> {
    return ConfigHolder.Node(
        config,
        chain.stack.dropWhile { it != this }.drop(1).firstOrNull() ?.storeHierarchy(),
        _subchains.mapNotNull {
            it.storeHierarchy()
        }
    )
}
