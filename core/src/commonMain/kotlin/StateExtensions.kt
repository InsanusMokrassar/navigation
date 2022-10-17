package dev.inmo.navigation.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

suspend fun <T, Node : NavigationNode<T>> Node.waitDetach() {
    chain.stackFlow.dropWhile {
        it.contains(this)
    }.first()
}

fun <T, Node : NavigationNode<T>> Node.detachJob(
    scope: CoroutineScope
): Job = scope.launch { waitDetach() }

suspend fun <O : Any, T, Node : NavigationNode<T>> Node.invokeOn(
    state: NavigationNodeState,
    callback: suspend (Node) -> O
): O? = stateChangesFlow.dropWhile {
    it.to != state && it.type != NavigationStateChange.Type.DESTROY
}.first().takeIf { it.to == state } ?.let {
    callback(this)
}

fun <O : Any, T, Node : NavigationNode<T>> Node.invokeOnAsync(
    state: NavigationNodeState,
    scope: CoroutineScope,
    callback: (Node) -> O
): Deferred<O?> = scope.async {
    invokeOn(state, callback)
}
