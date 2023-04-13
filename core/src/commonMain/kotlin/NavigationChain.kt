package dev.inmo.navigation.core

import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.common.diff
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NavigationChain<Base>(
    val parentNode: NavigationNode<out Base, Base>?,
    internal val nodeFactory: NavigationNodeFactory<Base>,
    val id: NavigationChainId? = null
) {
    private val log by lazy {
        TagLogger(toString())
    }
    private val nodesIds = mutableMapOf<NavigationNodeId, NavigationNode<out Base, Base>>()

    private val parentNodeState: NavigationNodeState
        get() = parentNode ?.state ?: NavigationNodeState.RESUMED

    private val _stackFlow = MutableStateFlow<List<NavigationNode<out Base, Base>>>(emptyList())
    val stackFlow: StateFlow<List<NavigationNode<out Base, Base>>> = _stackFlow.asStateFlow()
    internal val stack
        get() = stackFlow.value

    private val actualizeMutex = Mutex()
    private suspend fun actualizeStackStates() {
        val parentState = parentNodeState.also {
            log.d {
                "Chain state (parent $parentNode) now is $it"
            }
        }
        actualizeMutex.withLock {
            log.d { "Start actualization of stack $stack" }
            runCatchingSafely {
                stack.forEachIndexed { i, node ->
                    node.state = minOf(
                        parentState,
                        if (i == stack.lastIndex) {
                            NavigationNodeState.RESUMED
                        } else {
                            NavigationNodeState.STARTED
                        }
                    )

                    log.d { "$node actual state now is ${node.state}" }
                }
            }.onFailure {
                log.e(it) { "Unable to actualize stack of $this" }
            }
        }
    }

    fun push(config: Base): NavigationNode<out Base, Base>? {
        val newNode = nodeFactory.createNode(this, config) ?: let {
            log.d { "Unable to create node for $config" }
            return null
        }
        log.d { "Adding node $newNode with config $config" }
        nodesIds[newNode.id] = newNode
        _stackFlow.value += newNode
        log.d { "$newNode now in stack: $stack" }
        return newNode
    }

    fun pop(): NavigationNode<out Base, Base>? {
        return stack.lastOrNull() ?.apply {
            drop(id)
        }
    }

    fun drop(node: NavigationNode<*, Base>): NavigationNode<out Base, Base>? {
        var dropped = false

        val newStack = stack.filterNot { currentNode ->
            (currentNode === node).also {
                dropped = true
                log.d { "$currentNode (${currentNode.id}) is equal to ${node}: $it" }
            }
        }
        if (!dropped) {
            return null
        }

        node.state = NavigationNodeState.NEW

        log.d { "Stack before removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }
        _stackFlow.value = newStack
        log.d { "Stack after removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }

        nodesIds.remove(node.id)
        return node
    }

    fun drop(id: NavigationNodeId): NavigationNode<out Base, Base>? {
        return drop(nodesIds[id] ?: return null)
    }
    fun drop(id: String) = drop(NavigationNodeId(id))

    fun replace(
        node: NavigationNode<*, Base>,
        config: Base
    ): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        val i = stack.indexOfFirst { it === node }.takeIf { it > -1 } ?: return null

        nodesIds.remove(node.id)
        node.state = NavigationNodeState.NEW

        val newNode = nodeFactory.createNode(this, config) ?: return null

        _stackFlow.value = (stack.take(i) + newNode) + stack.drop(i + 1)
        nodesIds[newNode.id] = newNode

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

    fun clear() {
        while (stack.isNotEmpty()) {
            pop()
        }
    }
    fun dropItself(): Boolean {
        return parentNode ?.removeSubChain(this) == true
    }

    fun start(scope: CoroutineScope): Job {
        val subscope = scope.LinkedSupervisorScope()

        log.d { "Starting chain" }

        parentNode ?.run {
            (flowOf(state) + stateChangesFlow).subscribeSafelyWithoutExceptions(subscope) {
                log.d { "Start update of state due to parent state update to $it" }
                actualizeStackStates()
            }
        }

        parentNode ?.subchainsFlow ?.dropWhile { this in it } ?.subscribeSafelyWithoutExceptions(subscope) {
            log.d { "Cancelling subscope" }
            subscope.cancel()
            log.d { "Cancelled subscope" }
        }

        val nodeToJob = mutableMapOf<NavigationNodeId, Job>()
        val nodeToJobMutex = Mutex()

        merge(
            flow { emit(emptyList<NavigationNode<*, Base>>().diff(stackFlow.value)) },
            onNodesStackDiffFlow
        ).subscribeSafelyWithoutExceptions(subscope) {
            actualizeStackStates()
            nodeToJobMutex.withLock {
                it.removed.forEach { (i, it) ->
                    nodeToJob.remove(it.id) ?.cancel()
                }
                it.replaced.forEach { (old, new) ->
                    nodeToJob.remove(new.value.id) ?.cancel()
                    nodeToJob[new.value.id] = new.value.start(subscope)
                    nodeToJob.remove(old.value.id) ?.cancel()
                }
                it.added.forEach { (i, it) ->
                    nodeToJob.remove(it.id) ?.cancel()
                    nodeToJob[it.id] = it.start(subscope)
                }
            }

            if (stack.isEmpty() && it.removed.isNotEmpty()) {
                dropItself()
            }
        }

        return subscope.coroutineContext.job
    }
}
