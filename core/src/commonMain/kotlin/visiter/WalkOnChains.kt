package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.chainOrNodeEither
import dev.inmo.navigation.core.onChain

/**
 * Uses [walk] to visit whole navigation tree with [this] as root, but will pass in [onChain] lambda only visited
 * [NavigationChain]s
 */
inline fun <Base> ChainOrNodeEither<Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = walk { it.onChain(onChain) }

/**
 * Shortcut for main [ChainOrNodeEither].[walkOnChains]
 */
inline fun <Base> NavigationChain<Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = chainOrNodeEither().walkOnChains(onChain)

/**
 * Shortcut for main [ChainOrNodeEither].[walkOnChains]
 */
inline fun <Base> NavigationNode<out Base, Base>.walkOnChains(
    onChain: NavigationChainVisitingCallback<Base>
) = chainOrNodeEither().walkOnChains(onChain)
