package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.*

suspend fun <Base> ConfigHolder.Chain<Base>.restoreHierarchy(
    node: NavigationNode<out Base, Base>,
    dropRedundantChainsOnRestores: Boolean = false
): NavigationChain<Base> {
    val subchain = node.createEmptySubChain()

    firstNodeConfig.restoreHierarchy(subchain, dropRedundantChainsOnRestores)

    return subchain
}

suspend fun <Base> ConfigHolder.Chain<Base>.restoreHierarchy(
    factory: NavigationNodeFactory<Base>,
    chainToRestore: NavigationChain<Base> = NavigationChain(null, factory, id),
    dropRedundantChainsOnRestores: Boolean = false
): NavigationChain<Base> {
    firstNodeConfig.restoreHierarchy(chainToRestore, dropRedundantChainsOnRestores)

    return chainToRestore
}

suspend fun <Base> ConfigHolder.Node<Base>.restoreHierarchy(
    chain: NavigationChain<Base>,
    dropRedundantChainsOnRestores: Boolean = false
): NavigationNode<out Base, Base>? {
    val node = chain.push(config) ?: return null

    val createdSubchains = subchains.map {
        it.restoreHierarchy(node, dropRedundantChainsOnRestores)
    }

    if (dropRedundantChainsOnRestores) {
        node.subchains.forEach { subchain ->
            if (createdSubchains.any { it === subchain }) {
                return@forEach
            } else {
                subchain.clear()
            }
        }
    }

    subnode ?.restoreHierarchy(chain, dropRedundantChainsOnRestores)

    return node
}

suspend fun <Base> NavigationChain<Base>.restoreHierarchy(
    holder: ConfigHolder.Node<Base>,
    dropRedundantChainsOnRestores: Boolean = false
): NavigationNode<out Base, Base>? {
    return holder.restoreHierarchy(
        this,
        dropRedundantChainsOnRestores
    )
}

suspend fun <Base> NavigationNode<out Base, Base>.restoreHierarchy(
    holder: ConfigHolder.Chain<Base>,
    dropRedundantChainsOnRestores: Boolean = false,
): NavigationChain<Base> {
    return holder.restoreHierarchy(
        this,
        dropRedundantChainsOnRestores
    )
}

suspend fun <T> restoreHierarchy(
    holder: ConfigHolder<T>,
    factory: NavigationNodeFactory<T>,
    rootChain: NavigationChain<T> = NavigationChain(null, factory),
    dropRedundantChainsOnRestore: Boolean = false
): NavigationChain<T>? {
    return when (holder) {
        is ConfigHolder.Chain -> {
            holder.restoreHierarchy(
                factory,
                rootChain,
                dropRedundantChainsOnRestore
            )
        }
        is ConfigHolder.Node -> {
            holder.restoreHierarchy(
                rootChain,
                dropRedundantChainsOnRestore
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
            subchainsFlow.value.mapNotNull {
                it.storeHierarchy()
            }
        )
    } else {
        null
    }
}
