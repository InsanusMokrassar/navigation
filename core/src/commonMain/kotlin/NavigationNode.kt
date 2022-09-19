package dev.inmo.navigation.core

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

abstract class NavigationNode<T> {
    protected val log by lazy {
        logger
    }
    open val id: NavigationNodeId = NavigationNodeId()

    abstract val chain: NavigationChain<T>
    val chainHolder
        get() = chain

    internal val _subchains = mutableListOf<NavigationChain<T>>()
    protected val subchains: List<NavigationChain<T>> = _subchains
    private val _stateChanges = MutableSharedFlow<NavigationNodeState>(extraBufferCapacity = Int.MAX_VALUE)

    val stateChanges: Flow<NavigationNodeState> = _stateChanges.asSharedFlow()
    var state: NavigationNodeState = NavigationNodeState.NEW
        internal set(value) {
            val changes = NavigationStateChangeList(field, value)

            changes.forEach { change ->
                field = change.to

                if (change.isNegative) {
                    _stateChanges.tryEmit(change.to)
                }

                when {
                    change.onCreate -> onCreate()
                    change.onStart -> onStart()
                    change.onResume -> onResume()
                    change.onPause -> onPause()
                    change.onStop -> onStop()
                    change.onDestroy -> onDestroy()
                }

                if (change.isPositive) {
                    _stateChanges.tryEmit(change.to)
                }

                log.d { "State has been changed from ${change.from} to ${change.to}" }
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

    fun createSubChain(config: T): Pair<NavigationNode<T>, NavigationChain<T>>? {
        val newSubChain = NavigationChain(this, chain.scope.LinkedSupervisorScope(), chain.nodeFactory)
        _subchains.add(newSubChain)
        val createdNode = newSubChain.push(config) ?: return null
        newSubChain.stackFlow.subscribeSafelyWithoutExceptions(newSubChain.scope) {
            if (it.isEmpty() && _subchains.remove(newSubChain)) {
                newSubChain.scope.cancel()
            }
        }
        log.d { "Stack after adding of $config subchain: ${subchains.joinToString { it.stackFlow.value.joinToString { it.id.string } }}" }
        return createdNode to newSubChain
    }

    class Empty<T>(override val chain: NavigationChain<T>) : NavigationNode<T>()
}
