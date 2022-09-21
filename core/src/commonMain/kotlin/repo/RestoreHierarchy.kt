package dev.inmo.navigation.core.repo

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeState
import kotlinx.coroutines.flow.*

suspend fun <T> NavigationNode<T>.restoreHierarchy(
    configHolder: ConfigHolder<T>,
    expectedStateForRestoreContinue: NavigationNodeState? = NavigationNodeState.RESUMED
) {
    if (expectedStateForRestoreContinue != null) {
        stateChanges.takeWhile {
            it != expectedStateForRestoreContinue
        }.collect()
    }
    when (configHolder) {
        is ConfigHolder.Simple<T> -> {
            val node = chain.push(configHolder.config)

            configHolder.subconfig ?.let {
                node ?.restoreHierarchy(it, expectedStateForRestoreContinue)
            }
        }
        is ConfigHolder.Parent -> {
            configHolder.configs.forEach {
                val (newNode) = createSubChain(it.config) ?: return@forEach

                it.subconfig ?.let {
                    newNode.restoreHierarchy(it, expectedStateForRestoreContinue)
                }
            }
        }
    }
}
