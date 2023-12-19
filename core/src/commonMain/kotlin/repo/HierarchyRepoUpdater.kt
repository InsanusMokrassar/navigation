package dev.inmo.navigation.core.repo

import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.i
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun <T> NavigationConfigsRepo<T>.enableSavingHierarchy(
    listeningChain: NavigationChain<T>,
    scope: CoroutineScope,
    chainToSave: NavigationChain<T> = listeningChain.rootChain()
): Job {
    val logger = TagLogger("HierarchySaver")
    fun save(event: String) {
        logger.d { "Start saving of hierarchy on $event" }
        save(chainToSave.storeHierarchy() ?: return)
        logger.d { "Completed saving of hierarchy on $event" }
    }

    val nodeConfigsChangesJobs = mutableMapOf<NavigationNode<out T, T>, Job>()

    return listeningChain.onChangesInSubChains(
        scope = scope
    ) { chain, diff ->
        if (!chain.recursivelyStorableInNavigationHierarchy()) {
            logger.i { "Skip saving of hierarchy on change $diff. Reason: chain unsavable" }
            return@onChangesInSubChains
        }
        val eventsForSave = mutableSetOf<String>()

        diff.added.forEach { (_, it) ->
            if (it.stackStorableInNavigationHierarchy()) {
                eventsForSave.add("node adding (${it.config})")
                nodeConfigsChangesJobs[it] = it.configState.subscribeSafelyWithoutExceptions(scope) { _ ->
                    if (it.stackStorableInNavigationHierarchy()) {
                        save("config change")
                    }
                }
            }
        }

        diff.removed.forEach { (_, it) ->
            if ((it.config as? NavigationNodeDefaultConfig) ?.storableInNavigationHierarchy != false) {
                eventsForSave.add("node removing (${it.config})")
                nodeConfigsChangesJobs.remove(it) ?.cancel()
            }
        }

        if (eventsForSave.isNotEmpty()) {
            save(eventsForSave.joinToString("|"))
        }
    }
}

fun <T> NavigationChain<T>.enableSavingHierarchy(
    repo: NavigationConfigsRepo<T>,
    scope: CoroutineScope,
    chainToSave: NavigationChain<T> = rootChain()
): Job = repo.enableSavingHierarchy(this, scope, chainToSave)
