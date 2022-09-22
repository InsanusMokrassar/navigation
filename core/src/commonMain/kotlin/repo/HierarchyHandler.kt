package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.*
import kotlinx.coroutines.CoroutineScope
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

suspend fun <T> ConfigHolder.Chain<T>.restoreHierarchy(
    scope: CoroutineScope,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED,
    factory: NavigationNodeFactory<T>
): NavigationChain<T> {
    val subchain = NavigationChain(null, scope, factory)

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

suspend fun <T> NavigationChain<T>.restoreHierarchy(
    holder: ConfigHolder.Node<T>,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED
): NavigationNode<T>? {
    return holder.restoreHierarchy(
        this,
        expectedStateForRestoreContinue
    )
}

suspend fun <T> NavigationNode<T>.restoreHierarchy(
    holder: ConfigHolder.Chain<T>,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED
): NavigationChain<T> {
    return holder.restoreHierarchy(
        this,
        expectedStateForRestoreContinue
    )
}

suspend fun <T> restoreHierarchy(
    holder: ConfigHolder<T>,
    scope: CoroutineScope,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED,
    factory: NavigationNodeFactory<T>
): NavigationChain<T>? {
    return when (holder) {
        is ConfigHolder.Chain -> {
            holder.restoreHierarchy(
                scope,
                expectedStateForRestoreContinue,
                factory
            )
        }
        is ConfigHolder.Node -> {
            holder.restoreHierarchy(
                NavigationChain(null, scope, factory),
                expectedStateForRestoreContinue
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
