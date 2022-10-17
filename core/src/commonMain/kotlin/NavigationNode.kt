package dev.inmo.navigation.core

import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class NavigationNode<T> {
    protected val log by lazy {
        logger
    }
    open val id: NavigationNodeId = NavigationNodeId()

    abstract val chain: NavigationChain<T>
    abstract val config: T
    open val storableInNavigationHierarchy: Boolean
        get() = (config as? NavigationNodeDefaultConfig) ?.storableInNavigationHierarchy ?: true

    @Deprecated("Renamed", ReplaceWith("chain"))
    val chainHolder
        get() = chain

    internal val _subchains = mutableListOf<NavigationChain<T>>()
    protected val subchains: List<NavigationChain<T>>
        get() = _subchains.toList()
    internal val _subchainsFlow = MutableStateFlow<List<NavigationChain<T>>>(subchains)
    val subchainsFlow: StateFlow<List<NavigationChain<T>>> = _subchainsFlow.asStateFlow()
    private val _stateChanges = MutableSharedFlow<NavigationStateChange>(extraBufferCapacity = Int.MAX_VALUE)

    val stateChangesFlow: Flow<NavigationStateChange> = _stateChanges.asSharedFlow()
    val statesFlow: Flow<NavigationNodeState> = stateChangesFlow.map { it.to }
    var state: NavigationNodeState = NavigationNodeState.NEW
        internal set(value) {
            val changes = NavigationStateChangeList(field, value)

            changes.forEach { change ->
                field = change.to

                if (change.isNegative) {
                    _stateChanges.tryEmit(change)
                }

                when (change.type) {
                    NavigationStateChange.Type.CREATE -> onCreate()
                    NavigationStateChange.Type.START -> onStart()
                    NavigationStateChange.Type.RESUME -> onResume()
                    NavigationStateChange.Type.PAUSE -> onPause()
                    NavigationStateChange.Type.STOP -> onStop()
                    NavigationStateChange.Type.DESTROY -> onDestroy()
                }

                if (change.isPositive) {
                    _stateChanges.tryEmit(change)
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

    fun createEmptySubChain(): NavigationChain<T> {
        return NavigationChain(this, chain.nodeFactory).also {
            _subchains.add(it)
            _subchainsFlow.value = subchains
        }
    }

    protected fun removeChain(chain: NavigationChain<T>) {
        _subchains.remove(chain)
        _subchainsFlow.value = subchains
    }

    fun createSubChain(config: T): Pair<NavigationNode<T>, NavigationChain<T>>? {
        val newSubChain = createEmptySubChain()
        val createdNode = newSubChain.push(config) ?: return null
        log.d { "Stack after adding of $config subchain: ${subchains.joinToString { it.stackFlow.value.joinToString { it.id.string } }}" }
        return createdNode to newSubChain
    }

    open fun start(scope: CoroutineScope): Job {
        val subscope = scope.LinkedSupervisorScope()

        val chainToJob = mutableMapOf<NavigationChain<T>, Job>()
        val chainToJobMutex = Mutex()

        onChainAddedFlow.flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                chainToJob[it.value] = it.value.start(subscope)
                it.value.onNodeRemovedFlow.subscribeSafelyWithoutExceptions(subscope) { _ ->
                    if (it.value.stackFlow.value.isEmpty()) {
                        removeChain(it.value)
                    }
                }
            }
        }
        onChainRemovedFlow.flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                chainToJob.remove(it.value) ?.cancel()
            }
        }

        subscope.launch {
            subchainsFlow.value.forEach {
                chainToJobMutex.withLock {
                    chainToJob[it] = it.start(subscope)
                }
            }
        }

        return subscope.coroutineContext.job
    }

    class Empty<T>(override val chain: NavigationChain<T>, override val config: T) : NavigationNode<T>()
}
