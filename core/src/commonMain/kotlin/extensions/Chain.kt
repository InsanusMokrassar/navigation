package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.chainOrNodeEither
import kotlinx.coroutines.flow.*

val <Base> NavigationChain<Base>.onNodesStackDiffFlow: Flow<Diff<NavigationNode<out Base, Base>>>
    get() = flow {
        var previous = stack
        stackFlow.collect {
            val newValue = stackFlow.value
            emit(previous.diff(newValue, strictComparison = true))
            previous = newValue
        }
    }
val <Base> NavigationChain<Base>.onNodeAddedFlow
    get() = onNodesStackDiffFlow.map { it.added }.filter { it.isNotEmpty() }
val <Base> NavigationChain<Base>.onNodeRemovedFlow
    get() = onNodesStackDiffFlow.map { it.removed }.filter { it.isNotEmpty() }
val <Base> NavigationChain<Base>.onNodeReplacedFlow
    get() = onNodesStackDiffFlow.map { it.replaced }.filter { it.isNotEmpty() }

fun <Base> NavigationChain<Base>.rootChain(): NavigationChain<Base> = parentNode ?.chain ?.rootChain() ?: this

fun <Base> NavigationChain<Base>.findNode(id: NavigationNodeId): NavigationNode<*, Base>? = stackFlow.value.firstNotNullOfOrNull {
    it.findNode(id)
}

fun <Base> NavigationChain<Base>.findChain(id: NavigationChainId): NavigationChain<Base>? = if (this.id == id) {
    this
} else {
    stackFlow.value.firstNotNullOfOrNull { it.findChain(id) }
}


// Drop/replace/push by node id

fun <Base> NavigationChain<Base>.dropInSubTree(
    id: NavigationNodeId
): Boolean = chainOrNodeEither().dropInSubTree(id)

fun <Base> NavigationChain<Base>.dropNodeInSubTree(id: String) = chainOrNodeEither().dropNodeInSubTree(id)

fun <Base> NavigationChain<Base>.replaceInSubTree(
    id: NavigationNodeId,
    config: Base,
): Boolean = chainOrNodeEither().replaceInSubTree(id, config)

fun <Base> NavigationChain<Base>.replaceInSubTree(
    id: String,
    config: Base
) = chainOrNodeEither().replaceInSubTree(id, config)

fun <Base> NavigationChain<Base>.pushInSubTree(
    inChainWith: NavigationNodeId,
    config: Base
): Boolean = chainOrNodeEither().pushInSubTree(inChainWith, config)

fun <Base> NavigationChain<Base>.pushInSubTreeByNodeId(
    inChainWithNodeId: String,
    config: Base
) = chainOrNodeEither().pushInSubTreeByNodeId(inChainWithNodeId, config)

// Drop/push by chain id

fun <Base> NavigationChain<Base>.dropInSubTree(
    id: NavigationChainId
) = chainOrNodeEither().dropInSubTree(id)

fun <Base> NavigationChain<Base>.dropChainInSubTree(
    id: String
) = chainOrNodeEither().dropChainInSubTree(id)

fun <Base> NavigationChain<Base>.pushInSubTree(
    inChainWithNodeId: NavigationChainId,
    config: Base
) = chainOrNodeEither().pushInSubTree(inChainWithNodeId, config)

fun <Base> NavigationChain<Base>.pushInSubTreeByChainId(
    inChainWithNodeId: String,
    config: Base
) = chainOrNodeEither().pushInSubTreeByChainId(inChainWithNodeId, config)
