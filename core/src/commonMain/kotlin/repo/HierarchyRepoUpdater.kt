package dev.inmo.navigation.core.repo

import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

fun <T> NavigationConfigsRepo<T>.enableSavingHierarchy(
    listeningChain: NavigationChain<T>,
    scope: CoroutineScope,
    chainToSave: NavigationChain<T> = listeningChain.rootChain(),
    debounce: Long = 0L
): Job {
    val subscope = scope.LinkedSupervisorScope()
    val updatesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    updatesFlow.debounce(debounce).subscribeSafelyWithoutExceptions(subscope) {
        val hierarchy = chainToSave.storeHierarchy() ?: return@subscribeSafelyWithoutExceptions
        save(hierarchy)
    }

    fun NavigationNode<out T, T>.enableListeningUpdates(scope: CoroutineScope) {
        configState.subscribeSafelyWithoutExceptions(scope) {
            save(chainToSave.storeHierarchy() ?: return@subscribeSafelyWithoutExceptions)
        }
    }

    fun NavigationChain<T>.enableListeningUpdates() {
        stack.forEach {
            it.subchainsFlow.value.forEach {
                it.enableListeningUpdates()
            }
        }
        val currentSubscope = subscope.LinkedSupervisorScope()
        onNodeAddedFlow.flatten().subscribeSafelyWithoutExceptions(currentSubscope) { (_, it) ->
            save(chainToSave.storeHierarchy() ?: return@subscribeSafelyWithoutExceptions)
            it.enableListeningUpdates(currentSubscope)
            it.onChainAddedFlow.flatten().map { it.value }.subscribeSafelyWithoutExceptions(currentSubscope) {
                it.enableListeningUpdates()
            }
        }
        onNodeRemovedFlow.flatten().subscribeSafelyWithoutExceptions(currentSubscope) { _ ->
            save(chainToSave.storeHierarchy() ?: return@subscribeSafelyWithoutExceptions)
            currentSubscope.cancel()
        }
    }

    listeningChain.enableListeningUpdates()

    return subscope.coroutineContext.job
}

fun <T> NavigationChain<T>.enableSavingHierarchy(
    repo: NavigationConfigsRepo<T>,
    scope: CoroutineScope,
    chainToSave: NavigationChain<T> = rootChain(),
    debounce: Long = 0L
): Job = repo.enableSavingHierarchy(this, scope, chainToSave, debounce)
