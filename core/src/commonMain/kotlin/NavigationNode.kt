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

    internal val _subchainsFlow = SpecialMutableStateFlow<List<NavigationChain<Base>>>(emptyList())
    val subchainsFlow: StateFlow<List<NavigationChain<Base>>> = _subchainsFlow.asStateFlow()
    val subchains: List<NavigationChain<Base>>
        get() = _subchainsFlow.value.toList()
    private val _stateChanges = MutableSharedFlow<NavigationStateChange>(extraBufferCapacity = Int.MAX_VALUE)

    val stateChangesFlow: Flow<NavigationStateChange> = _stateChanges.asSharedFlow()
    val statesFlow: Flow<NavigationNodeState> = stateChangesFlow.map { it.to }
    var state: NavigationNodeState = NavigationNodeState.NEW
        private set

    private val changeStateMutex = Mutex()
    suspend fun changeState(newState: NavigationNodeState) {
        changeStateMutex.withLock {
            val changes = NavigationStateChangeList(state, newState)

            changes.forEach { change ->
                state = change.to

                if (change.isNegative) {
                    _stateChanges.tryEmit(change)
                }

                when (change.type) {
                    NavigationStateChange.Type.CREATE -> {
                        onBeforeCreate()
                        onCreate()
                    }
                    NavigationStateChange.Type.START -> {
                        onBeforeStart()
                        onStart()
                    }
                    NavigationStateChange.Type.RESUME -> {
                        onBeforeResume()
                        onResume()
                    }
                    NavigationStateChange.Type.PAUSE -> {
                        onBeforePause()
                        onPause()
                    }
                    NavigationStateChange.Type.STOP -> {
                        onBeforeStop()
                        onStop()
                    }
                    NavigationStateChange.Type.DESTROY -> {
                        onBeforeDestroy()
                        onDestroy()
                    }
                }

                if (change.isPositive) {
                    _stateChanges.tryEmit(change)
                }

                log.d { "State has been changed from ${change.from} to ${change.to}" }
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
    }

    private fun createEmptySubChainWithoutAttaching(id: NavigationChainId? = null): NavigationChain<Base> {
        return NavigationChain<Base>(this, chain.nodeFactory, id)
    }

    fun createEmptySubChain(id: NavigationChainId? = null): NavigationChain<Base> {
        return createEmptySubChainWithoutAttaching(id).also {
            _subchainsFlow.value += it
        }
    }

    fun removeSubChain(chain: NavigationChain<Base>): Boolean {
        log.d { "Removing chain $chain" }
        val containsChain = _subchainsFlow.value.any { it === chain }
        if (containsChain) {
            chain.clear()
        }
        _subchainsFlow.value = _subchainsFlow.value.filter { it !== chain }
        log.d { "Removed chain $chain" }
        return containsChain && !subchains.contains(chain)
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

        merge(
            onChainAddedFlow(initialStateOfSubchains),
            onChainReplacedFlow(initialStateOfSubchains).map {
                it.map { it.second }
            }
        ).flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                log.d { "Starting ${it.value}" }
                chainToJob[it.value] = it.value.start(subscope)
                log.d { "Started ${it.value}" }
            }
        }
        (onChainRemovedFlow(initialStateOfSubchains) + onChainReplacedFlow(initialStateOfSubchains).map {
            it.map { it.first }
        }).flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                log.d { "Cancelling and removing ${it.value}" }
                chainToJob.remove(it.value) ?.cancel()
                log.d { "Cancelled and removed ${it.value}" }
            }
        }

        onChainsStackDiffFlow(initialStateOfSubchains).subscribeSafelyWithoutExceptions(subscope) {
            log.d { it }
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
