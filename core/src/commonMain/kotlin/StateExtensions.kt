package dev.inmo.navigation.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

suspend fun <Base, Node : NavigationNode<out Base, Base>> Node.waitDetach() {
    chain.stackFlow.dropWhile {
        it.contains(this)
    }.first()
}

fun <Base, Node : NavigationNode<out Base, Base>> Node.detachJob(
    scope: CoroutineScope
): Job = scope.launch { waitDetach() }

suspend fun <O : Any, Base, Node : NavigationNode<out Base, Base>> Node.invokeOn(
    state: NavigationNodeState,
    callback: suspend (Node) -> O
): O? = stateChangesFlow.dropWhile {
    it.to != state && it.type != NavigationStateChange.Type.DESTROY
}.first().takeIf { it.to == state } ?.let {
    callback(this)
}

fun <O : Any, Base, Node : NavigationNode<out Base, Base>> Node.invokeOnAsync(
    state: NavigationNodeState,
    scope: CoroutineScope,
    callback: (Node) -> O
): Deferred<O?> = scope.async {
    invokeOn(state, callback)
}
