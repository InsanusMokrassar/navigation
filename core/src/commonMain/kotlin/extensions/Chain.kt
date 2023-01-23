package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
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
