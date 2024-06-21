package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.mapOnFirst
import dev.inmo.micro_utils.common.mapOnSecond
import dev.inmo.micro_utils.common.onPresented
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.visiter.walk
import dev.inmo.navigation.core.visiter.walkOnChains
import dev.inmo.navigation.core.visiter.walkOnNodes

// Find

/**
 * Will find and return FIRST element which will lead to true using [filter]
 */
inline fun <Base> ChainOrNodeEither<Base>.findInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): ChainOrNodeEither<Base>? {
    walk {
        if (filter(it)) {
            return it
        }
    }
    return null
}

/**
 * Will find and return FIRST [NavigationNode] which will lead to true using [filter]
 */
inline fun <Base> ChainOrNodeEither<Base>.findNodeInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): NavigationNode<*, Base>? {
    walk {
        return it.t2OrNull ?.takeIf(filter) ?: return@walk
    }
    return null
}

/**
 * Will find and return FIRST [NavigationChain] which will lead to true using [filter]
 */
inline fun <Base> ChainOrNodeEither<Base>.findChainInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): NavigationChain<Base>? {
    walk {
        return it.t1OrNull ?.takeIf(filter) ?: return@walk
    }
    return null
}

/**
 * Will find and return FIRST [NavigationNode] with [NavigationNode.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.findInSubTree(
    id: NavigationNodeId
): NavigationNode<*, Base>? = findNodeInSubTree { it.id == id }

/**
 * Will find and return FIRST [NavigationChain] with [NavigationChain.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.findInSubTree(
    id: NavigationChainId
): NavigationChain<Base>? = findChainInSubTree { it.id == id }

/**
 * Will find and return FIRST [NavigationNode] with [NavigationNode.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.findNodeInSubTree(
    id: String
): NavigationNode<*, Base>? = findInSubTree(NavigationNodeId(id))

/**
 * Will find and return FIRST [NavigationChain] with [NavigationChain.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.findChainInSubTree(
    id: String
): NavigationChain<Base>? = findInSubTree(NavigationChainId(id))

// Drop

/**
 * Will use [filter] to determine which [NavigationNode]/[NavigationChain] should be dropped from navigation tree
 *
 * * When [filter] returned true on chain - chain will be dropped (with [NavigationChain.dropItself])
 * * When [filter] returned true on node - node will be dropped using [NavigationChain.drop] of [NavigationNode.chain]
 */
inline fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): Boolean {
    var someDropped = false
    walk {
        val shouldBeDropped = filter(it)
        if (shouldBeDropped) {
            it.onChain {
                val dropped = it.dropItself()
                someDropped = dropped || someDropped
                return@walk
            }
            it.onNode {
                val dropped = it.chain.drop(it) != null
                someDropped = dropped || someDropped
                return@walk
            }
        }
    }
    return someDropped
}

/**
 * Will use [filter] when [dropInSubTree] pass [ChainOrNodeEither] with [NavigationNode]
 */
inline fun <Base> ChainOrNodeEither<Base>.dropNodesInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): Boolean = dropInSubTree {
    it.mapOnSecond(filter) ?: false
}

/**
 * Will use [filter] when [dropInSubTree] pass [ChainOrNodeEither] with [NavigationChain]
 */
inline fun <Base> ChainOrNodeEither<Base>.dropChainsInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): Boolean = dropInSubTree {
    it.mapOnFirst(filter) ?: false
}

/**
 * Will pass true each time when [dropNodesInSubTree] calls its mapper with [NavigationNode.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean = dropNodesInSubTree { it.id == id }

/**
 * Will pass true each time when [dropChainsInSubTree] calls its mapper with [NavigationChain.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationChainId
): Boolean = dropChainsInSubTree { it.id == id }

/**
 * Shortcut for method [ChainOrNodeEither].[dropInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.dropNodeInSubTree(id: String) = dropInSubTree(NavigationNodeId(id))

/**
 * Shortcut for method [ChainOrNodeEither].[dropInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.dropChainInSubTree(id: String) = dropInSubTree(NavigationChainId(id))

// Replace

/**
 * Will use [mapper] to determine in which chains or nodes should be replaced with new config and node
 *
 * * When [mapper] returned not null config on chain - chain will be dropped (with [NavigationChain.dropItself]) and new one will be pushed as subchain in parent
 * * When [mapper] returned not null config on node - node will be replaced with the new one
 */
inline fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean {
    var replaced = false
    walk {
        val newConfig = mapper(it) ?: return@walk

        it.onChain {
            val currentlyPushed = it.parentNode ?.createSubChain(newConfig)
            it.dropItself()
            replaced = currentlyPushed != null || replaced
            return@walk
        }
        it.onNode {
            val currentlyPushed = it.chain.replace(it, newConfig)
            replaced = currentlyPushed != null || replaced
            return@walk
        }
    }
    return replaced
}

/**
 * Shortcut for method [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.replaceNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = replaceInSubTree { it.t2OrNull ?.let(mapper) }

/**
 * Shortcut for method [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.replaceChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = replaceInSubTree { it.t1OrNull ?.let(mapper) }

/**
 * Will pass [config] each time when [replaceNodesInSubTree] calls its mapper with [NavigationNode.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = replaceNodesInSubTree { config.takeIf { _ -> it.id == id } }

/**
 * Will pass [config] each time when [replaceChainsInSubTree] calls its mapper with [NavigationChain.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = replaceChainsInSubTree { config.takeIf { _ -> it.id == id } }

/**
 * Shortcut for method [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.replaceNodesInSubTree(
    id: String,
    config: Base
) = replaceInSubTree(NavigationNodeId(id), config)

/**
 * Shortcut for method [ChainOrNodeEither].[replaceInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.replaceChainsInSubTree(
    id: String,
    config: Base
) = replaceInSubTree(NavigationChainId(id), config)

// Push

/**
 * Will use [mapper] to determine in which chains or nodes subchains push returned config
 *
 * * When [mapper] returned not null config on chain - new node will be pushed in chain
 * * When [mapper] returned not null config on node - new node will be pushed in sub chain
 */
inline fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    mapper: (ChainOrNodeEither<Base>) -> Base?
): Boolean {
    var pushed = false
    walk {
        val config = mapper(it) ?: return@walk

        it.onChain {
            val currentlyPushed = it.push(config)
            pushed = currentlyPushed != null || pushed
            return@walk
        }
        it.onNode {
            val currentlyPushed = it.createSubChain(config)
            pushed = currentlyPushed != null || pushed
            return@walk
        }
    }
    return pushed
}

/**
 * Shortcut for method [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInNodesInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean = pushInSubTree { it.t2OrNull ?.let(mapper) }

/**
 * Shortcut for method [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInChainsInSubTree(
    mapper: (NavigationChain<Base>) -> Base?
): Boolean = pushInSubTree { it.t1OrNull ?.let(mapper) }

/**
 * Will pass [config] each time when [pushInNodesInSubTree] calls its mapper with [NavigationNode.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean = pushInNodesInSubTree { config.takeIf { _ -> it.id == id } }

/**
 * Will pass [config] each time when [pushInChainsInSubTree] calls its mapper with [NavigationChain.id] == [id]
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean = pushInChainsInSubTree { config.takeIf { _ -> it.id == id } }

/**
 * Shortcut for method [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInNodesInSubTree(
    id: String,
    config: Base
) = pushInSubTree(NavigationNodeId(id), config)

/**
 * Shortcut for method [ChainOrNodeEither].[pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInChainsInSubTree(
    id: String,
    config: Base
) = pushInSubTree(NavigationChainId(id), config)
