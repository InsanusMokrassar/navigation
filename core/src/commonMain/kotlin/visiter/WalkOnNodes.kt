package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.chainOrNodeEither
import dev.inmo.navigation.core.onNode


/**
 * Uses [walk] to visit whole navigation tree with [this] as root, but will pass in [onNode] lambda only visited
 * [NavigationNode]s
 */
inline fun <Base> ChainOrNodeEither<Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = walk { it.onNode(onNode) }

/**
 * Shortcut for main [ChainOrNodeEither].[walkOnNodes]
 */
inline fun <Base> NavigationChain<Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = chainOrNodeEither().walkOnNodes(onNode)

/**
 * Shortcut for main [ChainOrNodeEither].[walkOnNodes]
 */
inline fun <Base> NavigationNode<out Base, Base>.walkOnNodes(
    onNode: NavigationNodeVisitingCallback<Base>
) = chainOrNodeEither().walkOnNodes(onNode)
