package dev.inmo.navigation.core.visiter

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.common.onFirst
import dev.inmo.micro_utils.common.onSecond
import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.chainOrNodeEither
import dev.inmo.navigation.core.onChain
import dev.inmo.navigation.core.onNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Will walk on whole tree of navigation. The order of walking next:
 *
 * * First faced [NavigationNode] will be passed to the [onNode] first
 * * First faced [NavigationChain] will be passed to the [onChain] first
 *
 * @receiver [Either] node or chain which assumed as a root for walking and will be used as first parameter for
 * calling of [onChain] or [onNode]
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
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
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationChain<Base>.walk(
    onNodeOrChain: NavigationNodeOrChainVisitingCallback<Base>
) = chainOrNodeEither().walk(onNodeOrChain)

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationNode<out Base, Base>.walk(
    onNodeOrChain: NavigationNodeOrChainVisitingCallback<Base>
) = chainOrNodeEither().walk(onNodeOrChain)

/**
 * Will walk on whole tree of navigation. The order of walking next:
 *
 * * First faced [NavigationNode] will be passed to the [onNode] first
 * * First faced [NavigationChain] will be passed to the [onChain] first
 *
 * @receiver [Either] node or chain which assumed as a root for walking and will be used as first parameter for
 * calling of [onChain] or [onNode]
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> ChainOrNodeEither<Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = walk { it.onChain(onChain) }

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationChain<Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = chainOrNodeEither().walkOnChains(onChain)

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationNode<out Base, Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = chainOrNodeEither().walkOnChains(onChain)

/**
 * Will walk on whole tree of navigation. The order of walking next:
 *
 * * First faced [NavigationNode] will be passed to the [onNode] first
 * * First faced [NavigationChain] will be passed to the [onChain] first
 *
 * @receiver [Either] node or chain which assumed as a root for walking and will be used as first parameter for
 * calling of [onChain] or [onNode]
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> ChainOrNodeEither<Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = walk { it.onNode(onNode) }

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationChain<Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = chainOrNodeEither().walkOnNodes(onNode)

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationNode<out Base, Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = chainOrNodeEither().walkOnNodes(onNode)

fun <Base> ChainOrNodeEither<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return flow {
        walk { emit(it) }
    }
}

fun <Base> NavigationChain<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return chainOrNodeEither().walkFlow()
}

fun <Base> NavigationNode<out Base, Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return chainOrNodeEither().walkFlow()
}
