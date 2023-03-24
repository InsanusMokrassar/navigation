package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationNode

typealias NavigationNodeOrChainVisitingCallback<Base> = (ChainOrNodeEither<Base>) -> Unit
