package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.flow.*

val <T> NavigationNode<T>.onChainsStackDiffFlow
    get() = flow {
        val previous = mutableListOf<NavigationChain<T>>()
        previous.applyDiff(subchainsFlow.value)
        subchainsFlow.collect {
            val newValue = subchainsFlow.value
            emit(previous.applyDiff(newValue, strictComparison = true))
        }
    }
val <T> NavigationNode<T>.onChainAddedFlow
    get() = onChainsStackDiffFlow.map { it.added + it.replaced.map { it.second } }.filter { it.isNotEmpty() }
val <T> NavigationNode<T>.onChainRemovedFlow
    get() = onChainsStackDiffFlow.map { it.removed }.filter { it.isNotEmpty() }
val <T> NavigationNode<T>.onChainReplacedFlow
    get() = onChainsStackDiffFlow.map { it.replaced }.filter { it.isNotEmpty() }
