package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.onPresented
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.visiter.walk
import dev.inmo.navigation.core.visiter.walkFlow
import dev.inmo.navigation.core.visiter.walkOnChains
import dev.inmo.navigation.core.visiter.walkOnNodes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

inline fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    filter: (ChainOrNodeEither<Base>) -> Boolean
): Boolean {
    var someDropped = false
    walk {
        val shouldBeDropped = filter(it)
        if (shouldBeDropped) {
            it.optionalT1.onPresented {
                val dropped = it.dropItself()
                someDropped = dropped || someDropped
            }
            it.optionalT2.onPresented {
                val dropped = it.chain.drop(it.id) != null
                someDropped = dropped || someDropped
            }
        }
    }
    return someDropped
}


// Drop/replace/push by chain id

/**
 * Will drop all chains in tree with [dev.inmo.navigation.core.NavigationChain.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
inline fun <Base> ChainOrNodeEither<Base>.dropNodeInSubTree(
    filter: (NavigationNode<*, Base>) -> Boolean
): Boolean {
    return dropInSubTree {
        it.t2OrNull ?.let(filter) == true
    }
}

/**
 * Will drop all nodes in tree with [dev.inmo.navigation.core.NavigationNode.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
inline fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean {
    return dropNodeInSubTree {
        it.id == id
    }
}

/**
 * Shortcut for [dropInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.dropNodeInSubTree(id: String) = dropInSubTree(NavigationNodeId(id))

/**
 * Will [dev.inmo.navigation.core.NavigationChain.replace] all nodes in tree with
 * [dev.inmo.navigation.core.NavigationNode.id] == [id] by a new one with [config]
 *
 * **This method will start its work with [this] as a root**
 */
inline fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    mapper: (NavigationNode<*, Base>) -> Base?
): Boolean {
    var replaced = false
    walkOnNodes {
        val newConfig = mapper(it)
        if (newConfig != null) {
            val currentlyReplaced = it.chain.replace(it, newConfig) != null
            replaced = currentlyReplaced || replaced
        }
    }
    return replaced
}

/**
 * Will [dev.inmo.navigation.core.NavigationChain.replace] all nodes in tree with
 * [dev.inmo.navigation.core.NavigationNode.id] == [id] by a new one with [config]
 *
 * **This method will start its work with [this] as a root**
 */
fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base,
): Boolean {
    var replaced = false
    walkOnChains {
        replaced = it.replace(id, config) != null || replaced
    }
    return replaced
}

/**
 * Shortcut for method [replaceInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    id: String,
    config: Base
) = replaceInSubTree(NavigationNodeId(id), config)

/**
 * Will push on top in all chains with any [dev.inmo.navigation.core.NavigationNode] in
 * [dev.inmo.navigation.core.NavigationChain.stack] with [dev.inmo.navigation.core.NavigationNode.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    id: NavigationNodeId,
    config: Base
): Boolean {
    var pushed = false
    walkOnNodes {
        if (it.id == id) {
            pushed = it.chain.push(config) != null || pushed
        }
    }
    return pushed
}

/**
 * Shortcut for method [pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTreeByNodeId(
    inChainWithNodeId: String,
    config: Base
) = pushInSubTree(NavigationNodeId(inChainWithNodeId), config)

// Drop/push by chain id

/**
 * Will drop all chains in tree with [dev.inmo.navigation.core.NavigationChain.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
inline fun <Base> ChainOrNodeEither<Base>.dropChainInSubTree(
    filter: (NavigationChain<Base>) -> Boolean
): Boolean {
    return dropInSubTree {
        it.t1OrNull ?.let(filter) == true
    }
}

/**
 * Will drop all chains in tree with [dev.inmo.navigation.core.NavigationChain.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationChainId
): Boolean {
    return dropChainInSubTree {
        it.id == id
    }
}

/**
 * Shortcut for method [dropInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.dropChainInSubTree(id: String) = dropInSubTree(NavigationChainId(id))

/**
 * Will push on top in all chains with [dev.inmo.navigation.core.NavigationChain.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    id: NavigationChainId,
    config: Base
): Boolean {
    var pushed = false
    walkOnChains {
        if (it.id == id) {
            pushed = it.push(config) != null || pushed
        }
    }
    return pushed
}

/**
 * Shortcut for method [pushInSubTree]
 */
fun <Base> ChainOrNodeEither<Base>.pushInSubTreeByChainId(
    inChainWithNodeId: String,
    config: Base
) = pushInSubTree(NavigationChainId(inChainWithNodeId), config)
