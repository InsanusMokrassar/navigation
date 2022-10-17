package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.flow.*


val <T> NavigationChain<T>.onNodesStackDiffFlow
    get() = flow {
        val previous = mutableListOf<NavigationNode<T>>()
        emit(previous.applyDiff(stackFlow.value, strictComparison = true))
        stackFlow.collect {
            val newValue = stackFlow.value
            emit(previous.applyDiff(newValue, strictComparison = true))
        }
    }
val <T> NavigationChain<T>.onNodeAddedFlow
    get() = onNodesStackDiffFlow.map { it.added }.filter { it.isNotEmpty() }
val <T> NavigationChain<T>.onNodeRemovedFlow
    get() = onNodesStackDiffFlow.map { it.removed }.filter { it.isNotEmpty() }
val <T> NavigationChain<T>.onNodeReplacedFlow
    get() = onNodesStackDiffFlow.map { it.replaced }.filter { it.isNotEmpty() }

fun <T> NavigationChain<T>.rootChain(): NavigationChain<T> = parentNode ?.chain ?.rootChain() ?: this
