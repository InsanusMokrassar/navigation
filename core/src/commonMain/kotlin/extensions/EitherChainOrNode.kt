package dev.inmo.navigation.core.extensions

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.visiter.walkOnChains
import dev.inmo.navigation.core.visiter.walkOnNodes


// Drop/replace/push by chain id

/**
 * Will drop all nodes in tree with [dev.inmo.navigation.core.NavigationNode.id] == [id]
 *
 * **This method will start its work with [this] as a root**
 */
fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean {
    var deleted = false
    walkOnNodes {
        if (it.id == id) {
            deleted = it.chain.drop(id) != null || deleted
        }
    }
    return deleted
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
fun <Base> ChainOrNodeEither<Base>.dropInSubTree(
    id: NavigationChainId
): Boolean {
    var deleted = false
    walkOnChains {
        if (it.id == id) {
            it.clear()
            deleted = true
        }
    }
    return deleted
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
