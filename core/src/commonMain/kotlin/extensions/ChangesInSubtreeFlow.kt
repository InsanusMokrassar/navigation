package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.*
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * @return [Flow] which will have one of two data variants: chain where happen changes and its stack list (list will be
 * presented only if change happen there); node and its subchaines (subchaines will be presented only if their list
 * have been changed)
 */
@Warning("This API is still experimental. Please, report on any wrong behaviour if any")
@OptIn(ExperimentalCoroutinesApi::class)
fun <Base> Either<NavigationChain<Base>, NavigationNode<out Base, Base>>.changesInSubtreeFlow(): Flow<
        Either<
                Pair<NavigationChain<Base>, List<NavigationNode<out Base, Base>>>,
                Pair<NavigationNode<out Base, Base>, List<NavigationChain<Base>>?>,
                >
        > {
    return merge(
        when (this) {
            is EitherFirst -> merge(
                // chain
                this.t1.stackFlow.map { (this.t1 to it).either() },
                this.t1.stackFlow.flatMapLatest {
                    it.map {
                        it.either<NavigationChain<Base>, NavigationNode<out Base, Base>>().changesInSubtreeFlow<Base>()
                    }.merge()
                },
            )

            is EitherSecond -> merge(
                // node
                this.t2.subchainsFlow.map { (this.t2 to it).either() },
                this.t2.subchainsFlow.flatMapLatest {
                    it.map {
                        it.either<NavigationChain<Base>, NavigationNode<out Base, Base>>().changesInSubtreeFlow<Base>()
                    }.merge()
                },
                this.t2.stateChangesFlow.map { (this.t2 to null).either() },
                this.t2.configState.map { (this.t2 to null).either() },
            )
        },
    )
}

@Warning("This API is still experimental. Please, report on any wrong behaviour if any")
fun <Base> NavigationChain<Base>.changesInSubTreeFlow() = either<NavigationChain<Base>, NavigationNode<out Base, Base>>().changesInSubtreeFlow()
@Warning("This API is still experimental. Please, report on any wrong behaviour if any")
fun <Base> NavigationNode<out Base, Base>.changesInSubTreeFlow() = either<NavigationChain<Base>, NavigationNode<out Base, Base>>().changesInSubtreeFlow()