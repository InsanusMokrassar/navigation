package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

suspend fun <Base> ConfigHolder.Chain<Base>.restoreHierarchy(
    node: NavigationNode<out Base, Base>
): NavigationChain<Base> {
    val subchain = node.createEmptySubChain()

    firstNodeConfig.restoreHierarchy(subchain)

    return subchain
}

suspend fun <Base> ConfigHolder.Chain<Base>.restoreHierarchy(
    factory: NavigationNodeFactory<Base>
): NavigationChain<Base> {
    val subchain = NavigationChain(null, factory)

    firstNodeConfig.restoreHierarchy(subchain)

    return subchain
}

suspend fun <Base> ConfigHolder.Node<Base>.restoreHierarchy(
    chain: NavigationChain<Base>,
): NavigationNode<out Base, Base>? {
    val node = chain.push(config) ?: return null

    subchains.forEach {
        it.restoreHierarchy(node)
    }

    subnode ?.restoreHierarchy(chain)

    return node
}

suspend fun <Base> NavigationChain<Base>.restoreHierarchy(
    holder: ConfigHolder.Node<Base>
): NavigationNode<out Base, Base>? {
    return holder.restoreHierarchy(
        this
    )
}

suspend fun <Base> NavigationNode<out Base, Base>.restoreHierarchy(
    holder: ConfigHolder.Chain<Base>,
): NavigationChain<Base> {
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

fun <Base> NavigationNode<out Base, Base>.storeHierarchy(): ConfigHolder.Node<Base>? {
    return if (storableInNavigationHierarchy) {
        ConfigHolder.Node(
            config,
            chain.stack.dropWhile { it != this }.drop(1).firstOrNull() ?.storeHierarchy(),
            _subchains.mapNotNull {
                it.storeHierarchy()
            }
        )
    } else {
        null
    }
}
