package dev.inmo.navigation.core.extensions

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.visiter.walk
import dev.inmo.navigation.core.visiter.walkOnChains
import dev.inmo.navigation.core.visiter.walkOnNodes


// Drop/replace/push by chain id

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

fun <Base> ChainOrNodeEither<Base>.dropNodeInSubTree(id: String) = dropInSubTree(NavigationNodeId(id))

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

fun <Base> ChainOrNodeEither<Base>.replaceInSubTree(
    id: String,
    config: Base
) = replaceInSubTree(NavigationNodeId(id), config)

fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    inChainWith: NavigationNodeId,
    config: Base
): Boolean {
    var pushed = false
    walkOnNodes {
        if (it.id == inChainWith) {
            pushed = it.chain.push(config) != null || pushed
        }
    }
    return pushed
}

fun <Base> ChainOrNodeEither<Base>.pushInSubTreeByNodeId(
    inChainWithNodeId: String,
    config: Base
) = pushInSubTree(NavigationNodeId(inChainWithNodeId), config)

// Drop/push by chain id

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

fun <Base> ChainOrNodeEither<Base>.dropChainInSubTree(id: String) = dropInSubTree(NavigationChainId(id))

fun <Base> ChainOrNodeEither<Base>.pushInSubTree(
    inChainWith: NavigationChainId,
    config: Base
): Boolean {
    var pushed = false
    walkOnChains {
        if (it.id == inChainWith) {
            pushed = it.push(config) != null || pushed
        }
    }
    return pushed
}

fun <Base> ChainOrNodeEither<Base>.pushInSubTreeByChainId(
    inChainWithNodeId: String,
    config: Base
) = pushInSubTree(NavigationChainId(inChainWithNodeId), config)
