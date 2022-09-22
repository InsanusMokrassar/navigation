package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

suspend fun <T> ConfigHolder.Chain<T>.restoreHierarchy(
    node: NavigationNode<T>,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED
): NavigationChain<T> {
    val subchain = node.createEmptySubChain()

    firstNodeConfig.restoreHierarchy(
        subchain,
        expectedStateForRestoreContinue
    )

    return subchain
}

suspend fun <T> ConfigHolder.Node<T>.restoreHierarchy(
    chain: NavigationChain<T>,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED
): NavigationNode<T>? {
    val node = chain.push(config) ?: return null

    if (expectedStateForRestoreContinue != null) {
        node.statesFlow.takeWhile {
            it != expectedStateForRestoreContinue
        }.collect()
    }

    subchains.forEach {
        it.restoreHierarchy(node, expectedStateForRestoreContinue)
    }

    subnode ?.restoreHierarchy(chain, expectedStateForRestoreContinue)

    return node
}

fun <T> NavigationChain<T>.storeHierarchy(): ConfigHolder.Chain<T>? {
    return ConfigHolder.Chain(
        stack.firstOrNull() ?.storeHierarchy() ?: return null
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
