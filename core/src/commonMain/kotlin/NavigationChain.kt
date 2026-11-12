package dev.inmo.navigation.core

import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.common.diff
import dev.inmo.micro_utils.common.withReplacedAt
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NavigationChain<Base>(
    val parentNode: NavigationNode<out Base, Base>?,
    internal val nodeFactory: NavigationNodeFactory<Base>,
    val id: NavigationChainId? = null
) {
    internal val lifetime: Job = SupervisorJob(parentNode?.lifetime)
    private val log by lazy {
        TagLogger(toString())
    }
    private val nodesIds = mutableMapOf<NavigationNodeId, NavigationNode<out Base, Base>>()

    private val parentNodeState: NavigationNodeState
        get() = parentNode ?.state ?: NavigationNodeState.RESUMED

    private val _stackFlow = MutableRedeliverStateFlow<List<NavigationNode<out Base, Base>>>(emptyList())
    val stackFlow: StateFlow<List<NavigationNode<out Base, Base>>> = _stackFlow.asStateFlow()
    internal val stack
        get() = stackFlow.value

    private fun actualizeStackStates() {
        val parentState = parentNodeState.also {
            log.d {
                "Chain state (parent $parentNode) now is $it"
            }
        }
        nodesChangesChannel.trySend {
            log.d { "Start actualization of stack $stack" }
            runCatchingLogging(logger = log) {
                stack.forEachIndexed { i, node ->
                    node.changeState(
                        minOf(
                            parentState,
                            if (i == stack.lastIndex) {
                                NavigationNodeState.RESUMED
                            } else {
                                NavigationNodeState.STARTED
                            }
                        )
                    )

                    log.d { "$node actual state now is ${node.state}" }
                }
            }.onFailure {
                log.e(it) { "Unable to actualize stack of $this" }
            }
        }
    }

    private val nodesChangesChannel: Channel<suspend () -> Unit> = Channel(Channel.UNLIMITED)
    private val nodesChangesMutex = Mutex()
    private var nodesChangesJobMutex = Mutex()
    private var nodesChangesJob: Job? = null
    private var isDestroyingSubtree = false

    init {
        lifetime.invokeOnCompletion { nodesChangesChannel.cancel() }
    }

    /**
     * Permanently releases this hierarchy's resources and cancels its navigation workers, including
     * nodes whose pushes are still queued. This is idempotent and does not run UI lifecycle callbacks
     * or wait for animations. Use it when the owner is destroyed, after saving any required state.
     *
     * For a resumable stop, cancel the job returned by [start] instead. A disposed chain cannot restart.
     */
    fun dispose() {
        lifetime.cancel()
    }

    private suspend fun destroySubtree() {
        nodesChangesMutex.withLock {
            // Run in the removing ancestor's scope, even if this chain has no
            // worker. Queued updates must not revive an explicitly removed subtree.
            isDestroyingSubtree = true
            stack.forEach { node ->
                node.changeState(minOf(node.state, NavigationNodeState.STARTED))
                node.subchains.forEach { it.destroySubtree() }
                node.changeState(NavigationNodeState.NEW)
                node.lifetime.cancel()
            }
        }
    }

    fun push(config: Base): NavigationNode<out Base, Base>? {
        if (!lifetime.isActive) return null
        val newNode = nodeFactory.createNode(this, config) ?: let {
            log.d { "Unable to create node for $config" }
            return null
        }
        nodesChangesChannel.trySend {
            log.d { "Adding node $newNode with config $config" }
            nodesIds[newNode.id] = newNode
            _stackFlow.value += newNode
            log.d { "$newNode now in stack: $stack" }
        }
        return newNode
    }

    fun pop(): NavigationNode<out Base, Base>? {
        return stack.lastOrNull() ?.apply {
            drop(id)
        }
    }

    fun drop(node: NavigationNode<*, Base>): NavigationNode<out Base, Base>? {
        if (!lifetime.isActive) return null
        val contains = stack.any { it === node }
        if (!contains) {
            return null
        }

        nodesChangesChannel.trySend {
            removeNode(node)
            if (stack.isEmpty()) {
                detachFromParent()
            }
        }
        return node
    }

    private suspend fun removeNode(node: NavigationNode<*, Base>) {
        if (stack.none { it === node }) return
        node.changeState(minOf(node.state, NavigationNodeState.STARTED))
        // Removing the parent cancels its descendants' jobs. Await their
        // serialized teardown first, including suspending lifecycle callbacks.
        node.subchains.forEach { it.destroySubtree() }

        log.d { "Stack before removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }
        _stackFlow.value = _stackFlow.value.filterNot { currentNode ->
            (currentNode === node).also {
                log.d { "$currentNode (${currentNode.id}) is equal to ${node}: $it" }
            }
        }
        log.d { "Stack after removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }

        node.changeState(NavigationNodeState.NEW)
        node.lifetime.cancel()
        nodesIds.remove(node.id)
    }

    private fun detachFromParent() {
        // Detaching cancels this chain's scope, so finish destruction first.
        parentNode?._subchainsFlow?.let { subchains ->
            subchains.value = subchains.value.filterNot { it === this }
            dispose()
        }
    }

    fun drop(id: NavigationNodeId): NavigationNode<out Base, Base>? {
        return drop(nodesIds[id] ?: return null)
    }
    fun drop(id: String) = drop(NavigationNodeId(id))

    fun replace(
        node: NavigationNode<*, Base>,
        config: Base
    ): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        if (!lifetime.isActive) return null
        val i = stack.indexOfFirst { it === node }.takeIf { it > -1 } ?: return null

        val newNode = nodeFactory.createNode(this, config) ?: return null

        nodesChangesChannel.trySend {
            val i = stack.indexOfFirst { it === node }.takeIf { it > -1 } ?: return@trySend
            node.changeState(minOf(node.state, NavigationNodeState.STARTED))
            node.subchains.forEach { it.destroySubtree() }
            node.changeState(NavigationNodeState.NEW)
            node.lifetime.cancel()

            nodesIds.remove(node.id)
            _stackFlow.value = stack.withReplacedAt(i) { newNode }
            nodesIds[newNode.id] = newNode
        }

        return node to newNode
    }

    fun reversedReplace(
        node: NavigationNode<*, Base>,
        config: Base
    ): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        if (!lifetime.isActive) return null
        val i = stack.indexOfFirst { it === node }.takeIf { it > -1 } ?: return null
        val newNode = nodeFactory.createNode(this, config) ?: return null

        nodesChangesChannel.trySend {
            val i = stack.indexOfFirst { it === node }.takeIf { it > -1 } ?: return@trySend
            _stackFlow.value = stack.withReplacedAt(i) { newNode }
            nodesIds.remove(node.id)
            nodesIds[newNode.id] = newNode

            node.changeState(minOf(node.state, NavigationNodeState.STARTED))
            node.subchains.forEach { it.destroySubtree() }
            node.changeState(NavigationNodeState.NEW)
            node.lifetime.cancel()
        }

        return node to newNode
    }

    fun replace(
        id: NavigationNodeId,
        config: Base
    ): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        return replace(stack.firstOrNull { it.id == id } ?: return null, config)
    }

    fun replace(
        id: String,
        config: Base
    ) = replace(NavigationNodeId(id), config)

    /** Enqueues removal of all nodes present when this command runs. */
    fun clear() {
        enqueueClear(detachEmptyChain = false)
    }

    internal fun clearAndDetach(): Boolean = enqueueClear(detachEmptyChain = true)

    private fun enqueueClear(detachEmptyChain: Boolean): Boolean = nodesChangesChannel.trySend {
        // Take the snapshot in the worker so earlier queued pushes are included.
        val nodes = stack.asReversed()
        nodes.forEach { removeNode(it) }
        if (nodes.isNotEmpty() || detachEmptyChain) {
            detachFromParent()
        }
    }.isSuccess

    /** Returns whether removal from the parent was accepted; teardown completes asynchronously. */
    fun dropItself(): Boolean {
        return parentNode ?.removeSubChain(this) == true
    }

    fun start(scope: CoroutineScope): Job {
        check(lifetime.isActive) { "Cannot start a disposed navigation chain" }
        val subscope = scope.LinkedSupervisorScope()
        val lifetimeHandle = lifetime.invokeOnCompletion { subscope.cancel() }
        subscope.coroutineContext.job.invokeOnCompletion { lifetimeHandle.dispose() }

        log.d { "Starting chain" }

        parentNode ?.run {
            stateFlow.subscribeLoggingDropExceptions(scope = subscope) {
                log.d { "Start update of state due to parent state update to $it" }
                actualizeStackStates()
            }
        }

        parentNode ?.subchainsFlow ?.dropWhile { this in it } ?.subscribeLoggingDropExceptions(scope = subscope) {
            log.d { "Cancelling subscope" }
            subscope.cancel()
            log.d { "Cancelled subscope" }
        }

        val nodeToJob = mutableMapOf<NavigationNodeId, Job>()
        val nodeToJobMutex = Mutex()

        onNodesStackDiffFlow(emptyList())
            .filter {
                !it.isEmpty()
            }
            .subscribeLoggingDropExceptions(scope = subscope) {
                actualizeStackStates()
                nodeToJobMutex.withLock {
                    it.removed.forEach { (_, it) ->
                        nodeToJob.remove(it.id) ?.cancel()
                    }
                    it.replaced.forEach { (old, new) ->
                        nodeToJob.remove(new.value.id) ?.cancel()
                        nodeToJob[new.value.id] = new.value.start(subscope)
                        nodeToJob.remove(old.value.id) ?.cancel()
                    }
                    it.added.forEach { (_, it) ->
                        nodeToJob.remove(it.id) ?.cancel()
                        nodeToJob[it.id] = it.start(subscope)
                    }
                }
            }

        subscope.launchLoggingDropExceptions {
            nodesChangesJobMutex.withLock {
                nodesChangesJob ?.cancel()
                nodesChangesJob = subscope.launch {
                    for (lambda in nodesChangesChannel) {
                        try {
                            nodesChangesMutex.withLock {
                                if (!isDestroyingSubtree) lambda()
                            }
                        } catch (failure: Throwable) {
                            // A callback may throw CancellationException without cancelling this worker.
                            // Only an actual job cancellation should stop later commands.
                            currentCoroutineContext().ensureActive()
                            log.e(failure) { "Unable to apply navigation chain update" }
                        }
                    }
                }
            }
        }

        return subscope.coroutineContext.job
    }

    override fun toString(): String {
        return "NavigationChain(id=$id, stack=$stack)"
    }

    companion object {
        operator fun <Base> invoke(
            parentNode: NavigationNode<out Base, Base>?,
            nodeFactory: NavigationNodeFactory<Base>,
            scope: CoroutineScope,
            id: NavigationChainId? = null
        ) = NavigationChain(parentNode = parentNode, nodeFactory = nodeFactory, id = id).apply {
            start(scope)
        }
    }
}
