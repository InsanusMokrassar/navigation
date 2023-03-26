package dev.inmo.navigation.core.visiter

import dev.inmo.micro_utils.common.onFirst
import dev.inmo.micro_utils.common.onSecond
import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.chainOrNodeEither


/**
 * Root fun for walking across the navigation tree starting with [this] as a root.
 *
 * **This function is not recursive**
 *
 * * Each met [NavigationNode] will be passed to [onNodeOrChain] and its [NavigationNode.subchains] will be added
 * __in the beginning__ of visiting queue
 * * Each met [NavigationChain] will be passed to [onNodeOrChain] and its [NavigationChain.stack] will be added
 * __in the beginning__ of visiting queue
 *
 * None happen of the [onNodeOrChain] exceptions will be stopped.
 *
 * @see walkFlow
 * @see walkOnChains
 * @see walkOnNodes
 */
inline fun <Base> ChainOrNodeEither<Base>.walk(
    onNodeOrChain: NavigationNodeOrChainVisitingCallback<Base>
) {
    val visitingQueue = ArrayDeque<ChainOrNodeEither<Base>>()
    visitingQueue.add(this)

    while (visitingQueue.isNotEmpty()) {
        val firstOne = visitingQueue.removeFirst()
        onNodeOrChain(firstOne)

        firstOne.onFirst {
            visitingQueue.addAll(0, it.stackFlow.value.map { it.chainOrNodeEither() })
        }.onSecond {
            visitingQueue.addAll(0, it.subchainsFlow.value.map { it.chainOrNodeEither() })
        }
    }
}

/**
 * Shortcut for main [ChainOrNodeEither].[walk]
 */
inline fun <Base> NavigationChain<Base>.walk(
    onNodeOrChain: NavigationNodeOrChainVisitingCallback<Base>
) = chainOrNodeEither().walk(onNodeOrChain)

/**
 * Shortcut for main [ChainOrNodeEither].[walk]
 */
inline fun <Base> NavigationNode<out Base, Base>.walk(
    onNodeOrChain: NavigationNodeOrChainVisitingCallback<Base>
) = chainOrNodeEither().walk(onNodeOrChain)
