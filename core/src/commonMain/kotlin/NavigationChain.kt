package dev.inmo.navigation.core

import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.common.diff
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NavigationChain<T>(
    internal val parentNode: NavigationNode<T>?,
    internal val nodeFactory: NavigationNodeFactory<T>
) {
    private val log = logger
    internal val stack = ArrayDeque<NavigationNode<T>>()
    private val nodesIds = mutableMapOf<NavigationNodeId, NavigationNode<T>>()

    private val parentNodeState: NavigationNodeState
        get() = parentNode ?.state ?: NavigationNodeState.RESUMED

    private val _stackFlow = MutableStateFlow<List<NavigationNode<T>>>(emptyList())
    val stackFlow: StateFlow<List<NavigationNode<T>>> = _stackFlow.asStateFlow()

    private val actualizeMutex = Mutex()
    private suspend fun actualizeStackStates() {
        val parentState = parentNodeState.also { log.d { "Parent state of $parentNode now is $it" } }
        actualizeMutex.withLock {
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

    fun push(config: T): NavigationNode<T>? {
        val newNode = nodeFactory.createNode(this, config) ?: return null
        stack.add(newNode)
        nodesIds[newNode.id] = newNode
        _stackFlow.value = stack.toList()
        return newNode
    }

    fun pop(): NavigationNode<T>? {
        val removed = stack.removeLastOrNull() ?.apply {
            nodesIds.remove(id)
            state = NavigationNodeState.NEW
            _stackFlow.value = stack.toList()
        }
        return removed
    }

    fun drop(id: NavigationNodeId): NavigationNode<T>? {
        val i = stack.indexOfFirst { it.id == id }.takeIf { it != -1 } ?: return null
        val oldNode = stack.removeAt(i)
        nodesIds.remove(id)
        oldNode.state = NavigationNodeState.NEW
        _stackFlow.value = stack.toList()
        return oldNode
    }
    fun drop(id: String) = drop(NavigationNodeId(id))

    fun replace(id: NavigationNodeId, config: T): Pair<NavigationNode<T>, NavigationNode<T>>? {
        val i = stack.indexOfFirst { it.id == id }.takeIf { it != -1 } ?: return null

        val newNode = nodeFactory.createNode(this, config) ?: return null
        val oldNode = stack.set(i, newNode)

        nodesIds.remove(id)
        nodesIds[newNode.id] = newNode

        oldNode.state = NavigationNodeState.NEW

        _stackFlow.value = stack.toList()
        return oldNode to newNode
    }

    fun replace(
        id: String, config: T
    ) = replace(NavigationNodeId(id), config)

    fun clear() {
        while (stack.isNotEmpty()) {
            pop()
        }
    }

    private fun doInTree(
        id: NavigationNodeId,
        visitedNodesChains: MutableSet<NavigationNodeId?>,
        actionName: String,
        onFound: NavigationChain<T>.() -> Unit
    ): Boolean {
        var found = false
        if (visitedNodesChains.add(parentNode ?.id)) {
            log.d { "Start $actionName for id $id in chain with stack ${nodesIds.keys.joinToString()} and parent node ${parentNode ?.id}" }
            if (nodesIds.containsKey(id)) {
                log.d { "Do $actionName for id $id in chain with stack ${nodesIds.keys.joinToString()} and parent node ${parentNode ?.id}" }
                onFound()
                found = true
            } else {
                log.d { "Unable to find node id $id in ${nodesIds.keys.joinToString()} for $actionName" }
            }

            (stack.flatMap { it._subchains } + listOfNotNull(parentNode ?.chain)).forEach { chainHolder ->
                found = chainHolder.doInTree(id, visitedNodesChains, actionName, onFound) || found
            }
        } else {
            log.d { "Visited again node ${parentNode ?.id} chain with id $id to find where to $actionName" }
        }

        return found
    }

    private fun dropInTree(
        id: NavigationNodeId,
        visitedNodesChains: MutableSet<NavigationNodeId?>
    ): Boolean = doInTree(id, visitedNodesChains, "drop") {
        drop(id)
    }

    fun dropInTree(id: NavigationNodeId) = dropInTree(id, mutableSetOf())
    fun dropInTree(id: String) = dropInTree(NavigationNodeId(id))

    private fun replaceInTree(
        id: NavigationNodeId,
        config: T,
        visitedNodesChains: MutableSet<NavigationNodeId?>
    ): Boolean = doInTree(id, visitedNodesChains, "replace") {
        replace(id, config)
    }

    fun replaceInTree(
        id: NavigationNodeId,
        config: T
    ) = replaceInTree(id, config, mutableSetOf())

    fun replaceInTree(
        id: String,
        config: T
    ) = replaceInTree(NavigationNodeId(id), config)

    private fun pushInTree(
        id: NavigationNodeId,
        config: T,
        visitedNodesChains: MutableSet<NavigationNodeId?>
    ): Boolean = doInTree(id, visitedNodesChains, "push") {
        push(config)
    }

    fun pushInTree(
        id: NavigationNodeId,
        config: T
    ) = pushInTree(id, config, mutableSetOf())

    fun pushInTree(
        id: String,
        config: T
    ) = pushInTree(NavigationNodeId(id), config)

    fun start(scope: CoroutineScope): Job {
        val subscope = scope.LinkedSupervisorScope()

        parentNode ?.run {
            (flowOf(state) + stateChangesFlow).subscribeSafelyWithoutExceptions(subscope) {
                log.d { "Start update of state due to parent state update to $it" }
                actualizeStackStates()
            }
        }

        stackFlow.dropWhile { it.isEmpty() }.subscribeSafelyWithoutExceptions(subscope) {
            if (it.isEmpty()) {
                subscope.cancel()
            } else {
                actualizeStackStates()
            }
        }

        val nodeToJob = mutableMapOf<NavigationNodeId, Job>()
        val nodeToJobMutex = Mutex()

        onNodeAddedFlow.flatten().subscribeSafelyWithoutExceptions(subscope) {
            nodeToJobMutex.withLock {
                nodeToJob[it.value.id] = it.value.start(subscope)
            }
        }
        onNodeRemovedFlow.flatten().subscribeSafelyWithoutExceptions(subscope) {
            nodeToJobMutex.withLock {
                nodeToJob.remove(it.value.id) ?.cancel()
            }
        }

        subscope.launch {
            stackFlow.value.forEach {
                nodeToJobMutex.withLock {
                    nodeToJob[it.id] = it.start(subscope)
                }
            }
            actualizeStackStates()
        }

        return subscope.coroutineContext.job
    }
}
