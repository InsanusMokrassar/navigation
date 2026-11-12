package dev.inmo.navigation.mvvm

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job

/**
 * This is an abstract class of ViewModel for MVVM pattern.
 *
 * Its [scope] is cancelled when the [NavigationNode] is destroyed or its owner disposes the hierarchy.
 * Stopping and restarting navigation workers preserves the model and its running work.
 */
abstract class ViewModel<T>(
    node: NavigationNode<out T, T>
) {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val log by lazy { logger }

    init {
        // A retained lifetime also handles models created after disposal, without a flow subscription
        // race. Cancellation is independent of Fragment attachment and suspending UI callbacks.
        val lifetimeHandle = node.lifetime.invokeOnCompletion { scope.cancel() }
        scope.coroutineContext.job.invokeOnCompletion { lifetimeHandle.dispose() }
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
