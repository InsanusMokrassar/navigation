package dev.inmo.navigation.mvvm

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * This is an abstract class of ViewModel for MVVM pattern.
 *
 * It has its own scope bind to [NavigationNode] lifecycle: when [NavigationNode] will be destroyed, its
 * [scope] will be cancelled
 */
abstract class ViewModel<T>(
    node: NavigationNode<out T, T>
) {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val log by lazy { logger }

    init {
        node.onDestroyFlow.subscribeSafelyWithoutExceptions(scope) {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    open fun onCreate() {
        log.d { "onCreate" }
    }
    open fun onStart() {
        log.d { "onStart" }
    }
    open fun onResume() {
        log.d { "onResume" }
    }
    open fun onPause() {
        log.d { "onPause" }
    }
    open fun onStop() {
        log.d { "onStop" }
    }
    open fun onDestroy() {
        log.d { "onDestroy" }
    }

    open suspend fun onBeforeCreate() {
        log.d { "onBeforeCreate" }
    }
    open suspend fun onBeforeStart() {
        log.d { "onBeforeStart" }
    }
    open suspend fun onBeforeResume() {
        log.d { "onBeforeResume" }
    }
    open suspend fun onBeforePause() {
        log.d { "onBeforePause" }
    }
    open suspend fun onBeforeStop() {
        log.d { "onBeforeStop" }
    }
    open suspend fun onBeforeDestroy() {
        log.d { "onBeforeDestroy" }
        if (scope.isActive) {
            scope.cancel()
        }
    }
}
