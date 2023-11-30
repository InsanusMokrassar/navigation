package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.diff
import dev.inmo.micro_utils.common.mapOnFirst
import dev.inmo.micro_utils.common.mapOnSecond
import dev.inmo.navigation.core.*
import kotlinx.coroutines.flow.*

/**
 * Start listening the nodes list changes. Result [Flow] is **cold** flow, so, you should be noticed
 * that the first state of this [Flow] is equal to [initial] OR (if null) the state of receiver
 * [NavigationChain.stack]
 *
 * If you want to receive info about changes including **current** state of
 * [NavigationChain.stack], you should pass [emptyList] into [initial] argument
 *
 * If you want in result [Flow] to always start from the creation time stack, pass
 * [NavigationChain.stack] of receiver as [initial] argument
 */
fun <Base> NavigationChain<Base>.onNodesStackDiffFlow(
    initial: List<NavigationNode<out Base, Base>>? = null
): Flow<Diff<NavigationNode<out Base, Base>>> {
    return flow {
        var previous: List<NavigationNode<out Base, Base>> = initial ?: stack.toList()
        stackFlow.collect {
            val newValue = stack
            val diff = previous.diff(newValue, strictComparison = true)
            emit(diff)
            previous = newValue.toList()
        }
    }
}
val <Base> NavigationChain<Base>.onNodesStackDiffFlow: Flow<Diff<NavigationNode<out Base, Base>>>
    get() = onNodesStackDiffFlow()
fun <Base> NavigationChain<Base>.onNodeAddedFlow(initial: List<NavigationNode<out Base, Base>>? = null) = onNodesStackDiffFlow(initial).map { it.added }.filter { it.isNotEmpty() }
val <Base> NavigationChain<Base>.onNodeAddedFlow
    get() = onNodeAddedFlow()
fun <Base> NavigationChain<Base>.onNodeRemovedFlow(initial: List<NavigationNode<out Base, Base>>? = null) = onNodesStackDiffFlow(initial).map { it.removed }.filter { it.isNotEmpty() }
val <Base> NavigationChain<Base>.onNodeRemovedFlow
    get() = onNodeRemovedFlow()
fun <Base> NavigationChain<Base>.onNodeReplacedFlow(initial: List<NavigationNode<out Base, Base>>? = null) = onNodesStackDiffFlow(initial).map { it.replaced }.filter { it.isNotEmpty() }
val <Base> NavigationChain<Base>.onNodeReplacedFlow
    get() = onNodeReplacedFlow()

fun <Base> NavigationChain<Base>.rootChain(): NavigationChain<Base> = parentNode ?.chain ?.rootChain() ?: this

fun <Base> NavigationChain<Base>.findNode(id: NavigationNodeId): NavigationNode<*, Base>? = stackFlow.value.firstNotNullOfOrNull {
    it.findNode(id)
}

fun <Base> NavigationChain<Base>.findChain(id: NavigationChainId): NavigationChain<Base>? = if (this.id == id) {
    this
} else {
    stackFlow.value.firstNotNullOfOrNull { it.findChain(id) }
}

// find

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): ChainOrNodeEither<Base>? = chainOrNodeEither().findInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findNodeInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findNodeInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): NavigationNode<*, Base>? = chainOrNodeEither().findNodeInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findChainInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findChainInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): NavigationChain<Base>? = chainOrNodeEither().findChainInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findInSubTree(
    id: NavigationNodeId
): NavigationNode<*, Base>? = chainOrNodeEither().findInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findInSubTree(
    id: NavigationChainId
): NavigationChain<Base>? = chainOrNodeEither().findInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findNodeInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findNodeInSubTree(
    id: String
): NavigationNode<*, Base>? = chainOrNodeEither().findNodeInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[findChainInSubTree]
 */
inline fun <Base> NavigationChain<Base>.findChainInSubTree(
    id: String
): NavigationChain<Base>? = chainOrNodeEither().findChainInSubTree(id)

// Drop

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationChain<Base>.dropInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): Boolean = chainOrNodeEither().dropInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropNodesInSubTree]
 */
inline fun <Base> NavigationChain<Base>.dropNodesInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): Boolean = chainOrNodeEither().dropNodesInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropChainsInSubTree]
 */
inline fun <Base> NavigationChain<Base>.dropChainsInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): Boolean = chainOrNodeEither().dropChainsInSubTree(filter)

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationChain<Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean = chainOrNodeEither().dropInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropInSubTree]
 */
inline fun <Base> NavigationChain<Base>.dropInSubTree(
    id: NavigationChainId
): Boolean = chainOrNodeEither().dropInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropNodeInSubTree]
 */
fun <Base> NavigationChain<Base>.dropNodeInSubTree(id: String) = chainOrNodeEither().dropNodeInSubTree(id)

/**
 * Shortcut for [ChainOrNodeEither].[dropChainInSubTree]
 */
fun <Base> NavigationChain<Base>.dropChainInSubTree(id: String) = chainOrNodeEither().dropChainInSubTree(id)

// Replace

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean = chainOrNodeEither().replaceInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceNodesInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = chainOrNodeEither().replaceNodesInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceChainsInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = chainOrNodeEither().replaceChainsInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceNodesInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceNodesInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceNodesInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[replaceChainsInSubTree]
 */
fun <Base> NavigationChain<Base>.replaceChainsInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceChainsInSubTree(id, config)

// Push

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean = chainOrNodeEither().pushInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInNodesInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = chainOrNodeEither().pushInNodesInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInChainsInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = chainOrNodeEither().pushInChainsInSubTree(mapper)

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInNodesInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInNodesInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().pushInNodesInSubTree(id, config)

/**
 * Shortcut for [ChainOrNodeEither].[pushInChainsInSubTree]
 */
fun <Base> NavigationChain<Base>.pushInChainsInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().pushInChainsInSubTree(id, config)
