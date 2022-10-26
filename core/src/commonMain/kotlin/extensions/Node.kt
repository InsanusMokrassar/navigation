package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.flow.*

val <Base> NavigationNode<out Base, Base>.onChainsStackDiffFlow
    get() = flow {
        val previous = mutableListOf<NavigationChain<Base>>()
        emit(previous.applyDiff(subchainsFlow.value, strictComparison = true))
        subchainsFlow.collect {
            val newValue = subchainsFlow.value
            emit(previous.applyDiff(newValue, strictComparison = true))
        }
    }
val <Base> NavigationNode<out Base, Base>.onChainAddedFlow
    get() = onChainsStackDiffFlow.map { it.added }.filter { it.isNotEmpty() }
val <Base> NavigationNode<out Base, Base>.onChainRemovedFlow
    get() = onChainsStackDiffFlow.map { it.removed }.filter { it.isNotEmpty() }
val <Base> NavigationNode<out Base, Base>.onChainReplacedFlow
    get() = onChainsStackDiffFlow.map { it.replaced }.filter { it.isNotEmpty() }
