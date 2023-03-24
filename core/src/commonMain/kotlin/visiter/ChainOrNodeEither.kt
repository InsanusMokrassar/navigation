package dev.inmo.navigation.core.visiter

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.either
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode

typealias ChainOrNodeEither<Base> = Either<NavigationChain<Base>, NavigationNode<out Base, Base>>

fun <Base> NavigationNode<out Base, Base>.chainOrNodeEither(): ChainOrNodeEither<Base> = either()
fun <Base> NavigationChain<Base>.chainOrNodeEither(): ChainOrNodeEither<Base> = either()
