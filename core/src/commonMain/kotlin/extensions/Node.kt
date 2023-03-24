package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.chainOrNodeEither
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


// Drop/replace/push by node id

fun <Base> NavigationNode<*, Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean = chainOrNodeEither().dropInSubTree(id)

fun <Base> NavigationNode<*, Base>.dropNodeInSubTree(id: String) = chainOrNodeEither().dropNodeInSubTree(id)

fun <Base> NavigationNode<*, Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base,
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

fun <Base> NavigationNode<*, Base>.replaceInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceInSubTree(id, config)

fun <Base> NavigationNode<*, Base>.pushInSubTree(
    inChainWith: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(inChainWith, config)

fun <Base> NavigationNode<*, Base>.pushInSubTreeByNodeId(
    inChainWithNodeId: String,
    config: Base
) = chainOrNodeEither().pushInSubTreeByNodeId(inChainWithNodeId, config)

// Drop/push by chain id

fun <Base> NavigationNode<*, Base>.dropInSubTree(
    id: NavigationChainId
) = chainOrNodeEither().dropInSubTree(id)

fun <Base> NavigationNode<*, Base>.dropChainInSubTree(
    id: String
) = chainOrNodeEither().dropChainInSubTree(id)

fun <Base> NavigationNode<*, Base>.pushInSubTree(
    inChainWithNodeId: NavigationChainId,
    config: Base
) = chainOrNodeEither().pushInSubTree(inChainWithNodeId, config)

fun <Base> NavigationNode<*, Base>.pushInSubTreeByChainId(
    inChainWithNodeId: String,
    config: Base
) = chainOrNodeEither().pushInSubTreeByChainId(inChainWithNodeId, config)
