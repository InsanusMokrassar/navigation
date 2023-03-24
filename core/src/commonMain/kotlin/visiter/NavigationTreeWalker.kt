package dev.inmo.navigation.core.visiter

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.common.onFirst
import dev.inmo.micro_utils.common.onSecond
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
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
    onNode: NavigationNodeVisitingCallback<Base> = {},
    onChain: NavigationChainVisitingCallback<Base> = {}
) {
    val visitingQueue = ArrayDeque<ChainOrNodeEither<Base>>()
    visitingQueue.add(this)

    while (visitingQueue.isNotEmpty()) {
        val firstOne = visitingQueue.removeFirst()

        firstOne.onFirst {
            onChain(it)
            visitingQueue.addAll(0, it.stackFlow.value.map { it.chainOrNodeEither() })
        }.onSecond {
            onNode(it)
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
    onNode: NavigationNodeVisitingCallback<Base> = {},
    onChain: NavigationChainVisitingCallback<Base> = {}
) = chainOrNodeEither().walk(onNode, onChain)

/**
 * Will walk on whole tree of navigation. **This function will start since [this] [NavigationChain]**
 *
 * @param onNode Will be passed in call [NavigationNode.walk]
 * @param onChain Will be called for [this] [NavigationChain] and passed in [NavigationNode.walk]
 */
inline fun <Base> NavigationNode<out Base, Base>.walk(
    onNode: NavigationNodeVisitingCallback<Base> = {},
    onChain: NavigationChainVisitingCallback<Base> = {}
) = chainOrNodeEither().walk(onNode, onChain)

fun <Base> ChainOrNodeEither<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return flow {
        walk(
            onNode = { emit(it.either()) },
            onChain = { emit(it.either()) }
        )
    }
}

fun <Base> NavigationChain<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return chainOrNodeEither().walkFlow()
}
