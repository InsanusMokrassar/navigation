package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.applyDiff
import dev.inmo.micro_utils.common.diff
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
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
