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

    internal val _subchainsFlow = MutableStateFlow<List<NavigationChain<Base>>>(emptyList())
    val subchainsFlow: StateFlow<List<NavigationChain<Base>>> = _subchainsFlow.asStateFlow()
    val subchains: List<NavigationChain<Base>>
        get() = _subchainsFlow.value.toList()
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

    fun createEmptySubChain(id: NavigationChainId? = null): NavigationChain<Base> {
        return NavigationChain<Base>(this, chain.nodeFactory, id).also {
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
        return containsChain && !subchains.contains(chain)
        log.d { "Removed chain $chain" }
    }

    fun createSubChain(config: Base, id: NavigationChainId? = null): Pair<NavigationNode<out Base, Base>, NavigationChain<Base>>? {
        val newSubChain = createEmptySubChain(id)
        val createdNode = newSubChain.push(config) ?: return null
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

        (onChainAddedFlow + onChainReplacedFlow.map { it.map { it.second } }).flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                log.d { "Starting ${it.value}" }
                chainToJob[it.value] = it.value.start(subscope)
                log.d { "Started ${it.value}" }
            }
        }
        (onChainRemovedFlow + onChainReplacedFlow.map { it.map { it.first } }).flatten().subscribeSafelyWithoutExceptions(subscope) {
            chainToJobMutex.withLock {
                log.d { "Cancelling and removing ${it.value}" }
                chainToJob.remove(it.value) ?.cancel()
                log.d { "Cancelled and removed ${it.value}" }
            }
        }

        onChainsStackDiffFlow.subscribeSafelyWithoutExceptions(subscope) {
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

    class Empty<T>(
        override val chain: NavigationChain<T>,
        config: T,
        id: NavigationNodeId = NavigationNodeId()
    ) : NavigationNode<T, T>(id) {
        override val configState: StateFlow<T> = MutableStateFlow(config).asStateFlow()
    }
}
