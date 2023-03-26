package dev.inmo.navigation.core

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.dataOrThrow
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.common.onFirst
import dev.inmo.micro_utils.common.onSecond

typealias ChainOrNodeEither<Base> = Either<NavigationChain<Base>, NavigationNode<out Base, Base>>

fun <Base> NavigationNode<out Base, Base>.chainOrNodeEither(): ChainOrNodeEither<Base> = either()
fun <Base> NavigationChain<Base>.chainOrNodeEither(): ChainOrNodeEither<Base> = either()

inline fun <Base> ChainOrNodeEither<Base>.onChain(
    onChain: (NavigationChain<Base>) -> Unit
) = onFirst(onChain)

inline fun <Base> ChainOrNodeEither<Base>.onNode(
    onNode: (NavigationNode<out Base, Base>) -> Unit
) = onSecond(onNode)

inline val <Base> ChainOrNodeEither<Base>.chainOrNull: NavigationChain<Base>?
    get() = t1OrNull

inline val <Base> ChainOrNodeEither<Base>.nodeOrNull: NavigationNode<out Base, Base>?
    get() = t2OrNull

inline val <Base> ChainOrNodeEither<Base>.chainOrThrow: NavigationChain<Base>
    get() = optionalT1.dataOrThrow(IllegalStateException("Value of $this should be chain"))

inline val <Base> ChainOrNodeEither<Base>.nodeOrThrow: NavigationNode<out Base, Base>
    get() = optionalT2.dataOrThrow(IllegalStateException("Value of $this should be node"))
