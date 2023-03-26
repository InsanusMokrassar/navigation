package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.chainOrNodeEither
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * Creates flow with visited [NavigationNode]s and [NavigationChain]s with [walk] throw whole tree using [this]
 * as root
 */
fun <Base> ChainOrNodeEither<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return flow {
        walk { emit(it) }
    }
}

/**
 * Shortcut for main [ChainOrNodeEither].[walkFlow]
 */
fun <Base> NavigationChain<Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return chainOrNodeEither().walkFlow()
}

/**
 * Shortcut for main [ChainOrNodeEither].[walkFlow]
 */
fun <Base> NavigationNode<out Base, Base>.walkFlow(): Flow<ChainOrNodeEither<Base>> {
    return chainOrNodeEither().walkFlow()
}
