package dev.inmo.navigation.core.repo

import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
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
    chainToSave: NavigationChain<T> = listeningChain.rootChain()
): Job {
    val logger = TagLogger("HierarchySaver")
    val subscope = scope.LinkedSupervisorScope()

    fun save(event: String) {
        logger.d { "Start saving of hierarchy on $event" }
        save(chainToSave.storeHierarchy() ?: return)
        logger.d { "Completed saving of hierarchy on $event" }
    }

    fun NavigationNode<out T, T>.enableListeningUpdates(scope: CoroutineScope) {
        configState.subscribeSafelyWithoutExceptions(scope) {
            save("config change")
        }
        onChainAddedFlow.subscribeSafelyWithoutExceptions(scope) {
            save("chain adding")
            enableListeningUpdates(scope)
        }
        onChainRemovedFlow.subscribeSafelyWithoutExceptions(scope) {
            save("chain removing")
        }
    }

    fun NavigationChain<T>.enableListeningUpdates() {
        val currentSubscope = subscope.LinkedSupervisorScope()
        onNodesStackDiffFlow.filter { it.isEmpty() }.subscribeSafelyWithoutExceptions(currentSubscope) {
            save("initialization")
        }
        onNodeAddedFlow.flatten().subscribeSafelyWithoutExceptions(currentSubscope) { (_, it) ->
            save("node adding")
            it.enableListeningUpdates(currentSubscope)
        }
        onNodeRemovedFlow.flatten().subscribeSafelyWithoutExceptions(currentSubscope) { _ ->
            save("node removing")
        }
        stack.forEach {
            it.enableListeningUpdates(currentSubscope)
        }
        if (stack.isNotEmpty()) {
            save("chain init")
        }
    }

    listeningChain.enableListeningUpdates()

    return subscope.coroutineContext.job
}

fun <T> NavigationChain<T>.enableSavingHierarchy(
    repo: NavigationConfigsRepo<T>,
    scope: CoroutineScope,
    chainToSave: NavigationChain<T> = rootChain()
): Job = repo.enableSavingHierarchy(this, scope, chainToSave)
