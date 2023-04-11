package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.diff
import dev.inmo.micro_utils.common.mapOnFirst
import dev.inmo.micro_utils.common.mapOnSecond
import dev.inmo.navigation.core.*
import kotlinx.coroutines.flow.*

val <Base> NavigationNode<out Base, Base>.onChainsStackDiffFlow: Flow<Diff<NavigationChain<Base>>>
    get() = flow {
        var previous = emptyList<NavigationChain<Base>>()
        subchainsFlow.collect {
            val newValue = subchainsFlow.value
            emit(previous.diff(newValue, strictComparison = true))
            previous = newValue
        }
    }
val <Base> NavigationNode<out Base, Base>.onChainAddedFlow
    get() = onChainsStackDiffFlow.map { it.added }.filter { it.isNotEmpty() }
val <Base> NavigationNode<out Base, Base>.onChainRemovedFlow
    get() = onChainsStackDiffFlow.map { it.removed }.filter { it.isNotEmpty() }
val <Base> NavigationNode<out Base, Base>.onChainReplacedFlow
    get() = onChainsStackDiffFlow.map { it.replaced }.filter { it.isNotEmpty() }

fun <Base> NavigationNode<*, Base>.findNode(id: NavigationNodeId): NavigationNode<*, Base>? = if (this.id == id) {
    this
} else {
    subchainsFlow.value.firstNotNullOfOrNull {
        it.findNode(id)
    }
}

fun <Base> NavigationNode<*, Base>.findChain(id: NavigationChainId): NavigationChain<Base>? = subchainsFlow.value.firstNotNullOfOrNull {
    it.findChain(id)
}

// find

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): ChainOrNodeEither<Base>? = chainOrNodeEither().findInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findNodeInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findNodeInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): NavigationNode<*, Base>? = chainOrNodeEither().findNodeInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findChainInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findChainInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): NavigationChain<Base>? = chainOrNodeEither().findChainInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findInSubTree(
    id: NavigationNodeId
): NavigationNode<*, Base>? = chainOrNodeEither().findInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findInSubTree(
    id: NavigationChainId
): NavigationChain<Base>? = chainOrNodeEither().findInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findNodeInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findNodeInSubTree(
    id: String
): NavigationNode<*, Base>? = chainOrNodeEither().findNodeInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findChainInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.findChainInSubTree(
    id: String
): NavigationChain<Base>? = chainOrNodeEither().findChainInSubTree(id)

// Drop

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.dropInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): Boolean = chainOrNodeEither().dropInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropNodesInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.dropNodesInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): Boolean = chainOrNodeEither().dropNodesInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropChainsInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.dropChainsInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): Boolean = chainOrNodeEither().dropChainsInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean = chainOrNodeEither().dropInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationNode<*, Base>.dropInSubTree(
    id: NavigationChainId
): Boolean = chainOrNodeEither().dropInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropNodeInSubTree]
 */
fun <Base> NavigationNode<*, Base>.dropNodeInSubTree(id: String) = chainOrNodeEither().dropNodeInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropChainInSubTree]
 */
fun <Base> NavigationNode<*, Base>.dropChainInSubTree(id: String) = chainOrNodeEither().dropChainInSubTree(id)

// Replace

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean = chainOrNodeEither().replaceInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceNodesInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = chainOrNodeEither().replaceNodesInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceChainsInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = chainOrNodeEither().replaceChainsInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceNodesInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceNodesInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceNodesInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceChainsInSubTree]
 */
fun <Base> NavigationNode<*, Base>.replaceChainsInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceChainsInSubTree(id, config)

// Push

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean = chainOrNodeEither().pushInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInNodesInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = chainOrNodeEither().pushInNodesInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInChainsInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = chainOrNodeEither().pushInChainsInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInNodesInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInNodesInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().pushInNodesInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInChainsInSubTree]
 */
fun <Base> NavigationNode<*, Base>.pushInChainsInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().pushInChainsInSubTree(id, config)
