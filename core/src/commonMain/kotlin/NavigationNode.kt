package dev.inmo.navigation.core

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

abstract class NavigationNode<Config : Base, Base>(
    open val id: NavigationNodeId = NavigationNodeId()
) {
    protected open val log: KSLog by lazy {
        TagLogger(toString())
    }

    abstract val chain: NavigationChain<Base>
    abstract val configState: StateFlow<Config>
    open val config: Config
        get() = configState.value
    open val storableInNavigationHierarchy: Boolean
        get() = (config as? NavigationNodeDefaultConfig) ?.storableInNavigationHierarchy ?: true

    internal val _subchainsFlow = MutableRedeliverStateFlow<List<NavigationChain<Base>>>(emptyList())
    val subchainsFlow: StateFlow<List<NavigationChain<Base>>> = _subchainsFlow.asStateFlow()
    val subchains: List<NavigationChain<Base>>
        get() = _subchainsFlow.value.toList()
    private val _statesFlow = MutableRedeliverStateFlow<NavigationNodeState>(NavigationNodeState.NEW)
    val stateFlow = _statesFlow.asStateFlow()
    private val _stateChanges = MutableSharedFlow<NavigationStateChange>(extraBufferCapacity = Int.MAX_VALUE)

    val stateChangesFlow: Flow<NavigationStateChange> = _stateChanges.asSharedFlow()
    @Deprecated("This field is deprecated and will be removed soon", ReplaceWith("stateFlow"))
    val statesFlow: Flow<NavigationNodeState>
        get() = stateFlow
    val state: NavigationNodeState
        get() = _statesFlow.value

    private val changeStateMutex = Mutex()
    private class StateChangeContext(key: CoroutineContext.Key<StateChangeContext>) : AbstractCoroutineContextElement(key)
    private val changeStateContextKey = object : CoroutineContext.Key<StateChangeContext> {}

    /**
     * Transitions this node to the specified state by applying each required adjacent lifecycle change.
     *
     * Lifecycle callbacks are invoked before and after every transition step. State change events are
     * emitted before callbacks for downward transitions and after callbacks for upward transitions.
     * Transitions are serialized to prevent concurrent state changes. Recursive calls for this node
     * from its active transition's coroutine context (including child coroutines) are rejected.
     *
     * @param newState The target lifecycle state for this node.
     * @throws IllegalStateException if this node already has an active transition in the caller's context.
     */
    suspend fun changeState(newState: NavigationNodeState) {
        val inheritedChange = currentCoroutineContext()[changeStateContextKey]
        // A child may retain this context after the transition ends; reject only an active owner.
        check(inheritedChange == null || !changeStateMutex.holdsLock(inheritedChange)) {
            "Reentrant changeState is not allowed for node $id during its lifecycle callback"
        }
        val owner = StateChangeContext(changeStateContextKey)
        changeStateMutex.withLock(owner) {
            // Carry failures across the context boundary without coroutine exception wrapping.
            withContext(owner) {
                runCatching {
                    val changes = NavigationStateChangeList(state, newState)

                    changes.forEach { change ->
                        when (change.type) {
                            NavigationStateChange.Type.CREATE -> {
                                onBeforeCreate()
                            }
                            NavigationStateChange.Type.START -> {
                                onBeforeStart()
                            }
                            NavigationStateChange.Type.RESUME -> {
                                onBeforeResume()
                            }
                            NavigationStateChange.Type.PAUSE -> {
                                onBeforePause()
                            }
                            NavigationStateChange.Type.STOP -> {
                                onBeforeStop()
                            }
                            NavigationStateChange.Type.DESTROY -> {
                                onBeforeDestroy()
                            }
                        }
                        _statesFlow.value = change.to

                        if (change.isNegative) {
                            _stateChanges.tryEmit(change)
                        }

                        when (change.type) {
                            NavigationStateChange.Type.CREATE -> {
                                onCreate()
                            }
                            NavigationStateChange.Type.START -> {
                                onStart()
                            }
                            NavigationStateChange.Type.RESUME -> {
                                onResume()
                            }
                            NavigationStateChange.Type.PAUSE -> {
                                onPause()
                            }
                            NavigationStateChange.Type.STOP -> {
                                onStop()
                            }
                            NavigationStateChange.Type.DESTROY -> {
                                onDestroy()
                            }
                        }

                        if (change.isPositive) {
                            _stateChanges.tryEmit(change)
                        }

                        log.d { "State has been changed from ${change.from} to ${change.to}" }
                    }
                }
            }.getOrThrow()
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
    }

    private fun createEmptySubChainWithoutAttaching(id: NavigationChainId? = null): NavigationChain<Base> {
        return NavigationChain<Base>(this, chain.nodeFactory, id)
    }

    fun createEmptySubChain(id: NavigationChainId? = null): NavigationChain<Base> {
        return createEmptySubChainWithoutAttaching(id).also {
            _subchainsFlow.value += it
        }
    }

    /**
     * Returns whether removal was accepted; the chain stays attached until its nodes are destroyed.
     * Removal runs on the chain's worker after preceding commands, so an unstarted chain must be started.
     */
    fun removeSubChain(chain: NavigationChain<Base>): Boolean {
        log.d { "Removing chain $chain" }
        val containsChain = _subchainsFlow.value.any { it === chain }
        return containsChain && chain.clearAndDetach()
    }

    fun createSubChain(config: Base, id: NavigationChainId? = null): Pair<NavigationNode<out Base, Base>, NavigationChain<Base>>? {
        val newSubChain = createEmptySubChainWithoutAttaching(id)
        val createdNode = newSubChain.push(config) ?: return null
        _subchainsFlow.value += newSubChain
        log.d { "Stack after adding of $config subchain: ${subchains.joinToString("; ") { it.stackFlow.value.joinToString { it.id.string } }}" }
        return createdNode to newSubChain
    }

    fun createSubChain(id: NavigationChainId, config: Base): Pair<NavigationNode<out Base, Base>, NavigationChain<Base>>? {
        return createSubChain(config, id)
    }

    open fun start(scope: CoroutineScope): Job {
        val subscope = scope.LinkedSupervisorScope()

        val chainToJob = mutableMapOf<NavigationChain<Base>, Job>()
        val chainToJobMutex = Mutex()

        val initialStateOfSubchains = subchains

        fun actualizeSubchainWithoutLock(chain: NavigationChain<Base>) {
            log.d { "Starting ${chain}" }
            chainToJob[chain] ?.cancel()
            chainToJob[chain] = chain.start(subscope)
            log.d { "Started ${chain}" }
        }
        onChainAddedFlow(initialStateOfSubchains).flatten().subscribeLoggingDropExceptions(subscope) {
            chainToJobMutex.withLock {
                actualizeSubchainWithoutLock(it.value)
            }
        }
        onChainReplacedFlow(initialStateOfSubchains).subscribeLoggingDropExceptions(subscope) {
            chainToJobMutex.withLock {
                it.forEach {
                    chainToJob.remove(it.first.value) ?.cancel()
                    actualizeSubchainWithoutLock(it.second.value)
                }
            }
        }
        (onChainRemovedFlow(initialStateOfSubchains) + onChainReplacedFlow(initialStateOfSubchains).map {
            it.map { it.first }
        }).flatten().subscribeLoggingDropExceptions(subscope) {
            chainToJobMutex.withLock {
                log.d { "Cancelling and removing ${it.value}" }
                chainToJob.remove(it.value) ?.cancel()
                log.d { "Cancelled and removed ${it.value}" }
            }
        }

        onChainsStackDiffFlow(initialStateOfSubchains).subscribeLoggingDropExceptions(subscope) {
            log.d { it }
        }

        subscope.launch {
            subchainsFlow.value.forEach {
                chainToJobMutex.withLock {
                    actualizeSubchainWithoutLock(it)
                }
            }
        }

        return subscope.coroutineContext.job
    }

    override fun toString(): String {
        return "NavigationNode(id=$id, subchains=${subchains}, config=$config)"
    }

    class Empty<T : Base, Base>(
        override val chain: NavigationChain<Base>,
        config: T,
        id: NavigationNodeId = NavigationNodeId()
    ) : NavigationNode<T, Base>(id) {
        object Config
        override val configState: StateFlow<T> = MutableStateFlow(config).asStateFlow()

        companion object {
            val DefaultFactory = NavigationNodeFactory.Typed<Config, Any?> { chain, config ->
                Empty(chain, config)
            }
        }
    }
}
