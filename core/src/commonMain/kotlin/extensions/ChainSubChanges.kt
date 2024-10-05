package dev.inmo.navigation.core.extensions

import dev.inmo.micro_utils.common.*
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun <Base> NavigationChain<Base>.onChangesInSubTree(
    scope: CoroutineScope,
    onChangeInSubChain: suspend (NavigationChain<Base>, Diff<NavigationNode<out Base, Base>>) -> Unit,
    onChangeInSubNode: suspend (NavigationNode<out Base, Base>, Diff<NavigationChain<Base>>) -> Unit
): Job {
    val subscope = scope.LinkedSupervisorScope()
    val listeningJobs = mutableMapOf<NavigationNodeId, Job>()
    onNodesStackDiffFlow(emptyList()).subscribeSafelyWithoutExceptions(subscope) {
        it.removed.forEach { (_, it) ->
            listeningJobs.remove(it.id) ?.cancel()
        }
        listeningJobs.putAll(
            it.added.associate { (_, it) ->
                it.id to it.onChangesInSubTree(subscope, onChangeInSubNode, onChangeInSubChain)
            }
        )
        onChangeInSubChain(this@onChangesInSubTree, it)
    }
    parentNode ?.onChainRemovedFlow(emptyList()) ?.filter {
        it.any { it.value === this@onChangesInSubTree }
    } ?.subscribeSafelyWithoutExceptions(subscope) {
        subscope.cancel()
    }

    return subscope.coroutineContext.job
}

fun <Config : Base, Base> NavigationNode<Config, Base>.onChangesInSubTree(
    scope: CoroutineScope,
    onChangeInSubNode: suspend (NavigationNode<out Base, Base>, Diff<NavigationChain<Base>>) -> Unit,
    onChangeInSubChain: suspend (NavigationChain<Base>, Diff<NavigationNode<out Base, Base>>) -> Unit
): Job {
    val subscope = scope.LinkedSupervisorScope()
    val listeningJobs = mutableMapOf<NavigationChain<Base>, Job>()

    onChainsStackDiffFlow(emptyList()).subscribeSafelyWithoutExceptions(subscope) {
        it.removed.forEach { (_, it) ->
            listeningJobs.remove(it) ?.cancel()
        }
        listeningJobs.putAll(
            it.added.associate { (_, it) ->
                it to it.onChangesInSubTree(subscope, onChangeInSubChain, onChangeInSubNode)
            }
        )
        onChangeInSubNode(this@onChangesInSubTree, it)
    }
    configState.subscribeSafelyWithoutExceptions(subscope) {

    }

    onDestroyFlow.subscribeSafelyWithoutExceptions(subscope) {
        subscope.cancel()
    }

    return subscope.coroutineContext.job
}

fun <Base> Either<NavigationChain<Base>, NavigationNode<out Base, Base>>.onChangesInSubTree(
    scope: CoroutineScope,
    onChangeInSubChainOrNode: suspend (Either<NavigationChain<Base>, NavigationNode<out Base, Base>>, Diff<Either<NavigationChain<Base>, NavigationNode<out Base, Base>>>) -> Unit
): Job {
    val onChangeInSubChain: suspend (NavigationChain<Base>, Diff<NavigationNode<out Base, Base>>) -> Unit = { chain, diff ->
        onChangeInSubChainOrNode(
            chain.either(),
            Diff(
                removed = diff.removed.map {
                    IndexedValue(
                        it.index,
                        it.value.either()
                    )
                },
                added = diff.added.map {
                    IndexedValue(
                        it.index,
                        it.value.either()
                    )
                },
                replaced = diff.replaced.map {
                    Pair(
                        IndexedValue(
                            it.second.index,
                            it.second.value.either()
                        ),
                        IndexedValue(
                            it.second.index,
                            it.second.value.either()
                        )
                    )
                }
            )
        )
    }
    val onChangeInSubNode: suspend (NavigationNode<out Base, Base>, Diff<NavigationChain<Base>>) -> Unit = { node, diff ->
        onChangeInSubChainOrNode(
            node.either(),
            Diff(
                removed = diff.removed.map {
                    IndexedValue(
                        it.index,
                        it.value.either()
                    )
                },
                added = diff.added.map {
                    IndexedValue(
                        it.index,
                        it.value.either()
                    )
                },
                replaced = diff.replaced.map {
                    Pair(
                        IndexedValue(
                            it.second.index,
                            it.second.value.either()
                        ),
                        IndexedValue(
                            it.second.index,
                            it.second.value.either()
                        )
                    )
                }
            )
        )
    }

    return mapOnFirst {
        it.onChangesInSubTree(scope, onChangeInSubChain, onChangeInSubNode)
    } ?: mapOnSecond {
        it.onChangesInSubTree(scope, onChangeInSubNode, onChangeInSubChain)
    } ?: error("$this must contains node or chain")
}

fun <Base> NavigationChain<Base>.onChangesInSubTree(
    scope: CoroutineScope,
    onChangeInSubChainOrNode: suspend (Either<NavigationChain<Base>, NavigationNode<out Base, Base>>, Diff<Either<NavigationChain<Base>, NavigationNode<out Base, Base>>>) -> Unit
) = either<NavigationChain<Base>, NavigationNode<out Base, Base>>().onChangesInSubTree(
    scope,
    onChangeInSubChainOrNode
)

fun <Base> NavigationNode<out Base, Base>.onChangesInSubTree(
    scope: CoroutineScope,
    onChangeInSubChainOrNode: suspend (Either<NavigationChain<Base>, NavigationNode<out Base, Base>>, Diff<Either<NavigationChain<Base>, NavigationNode<out Base, Base>>>) -> Unit
) = this.either<NavigationChain<Base>, NavigationNode<out Base, Base>>().onChangesInSubTree(
    scope,
    onChangeInSubChainOrNode
)

fun <Base> NavigationChain<Base>.onChangesInSubChains(
    scope: CoroutineScope,
    onChangeInSubChain: suspend (NavigationChain<Base>, Diff<NavigationNode<out Base, Base>>) -> Unit
) = onChangesInSubTree(scope, onChangeInSubChain, { _, _ -> /* do nothing */})

fun <Base> NavigationChain<Base>.onChangesInSubNodes(
    scope: CoroutineScope,
    onChangeInSubNode: suspend (NavigationNode<out Base, Base>, Diff<NavigationChain<Base>>) -> Unit
) = onChangesInSubTree(scope, { _, _ -> /* do nothing */}, onChangeInSubNode)

fun <Base> NavigationNode<out Base, Base>.onChangesInSubChains(
    scope: CoroutineScope,
    onChangeInSubChain: suspend (NavigationChain<Base>, Diff<NavigationNode<out Base, Base>>) -> Unit
) = onChangesInSubTree(scope, { _, _ -> /* do nothing */}, onChangeInSubChain)

fun <Base> NavigationNode<out Base, Base>.onChangesInSubNodes(
    scope: CoroutineScope,
    onChangeInSubNode: suspend (NavigationNode<out Base, Base>, Diff<NavigationChain<Base>>) -> Unit
) = onChangesInSubTree(scope, onChangeInSubNode, { _, _ -> /* do nothing */})
