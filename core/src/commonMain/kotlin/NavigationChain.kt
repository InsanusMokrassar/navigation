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
    internal val parentNode: NavigationNode<out Base, Base>?,
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

    fun drop(id: NavigationNodeId): NavigationNode<out Base, Base>? {
        val node = nodesIds[id] ?: return null
        node.state = NavigationNodeState.NEW
        log.d { "Stack before removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }
        _stackFlow.value = _stackFlow.value.filterNot { currentNode ->
            (currentNode.id == id).also {
                log.d { "Id of $currentNode (${currentNode.id}) is equal to $id: $it" }
            }
        }
        log.d { "Stack after removing of $node: ${stackFlow.value.joinToString("; ") { it.toString() }}" }
        nodesIds.remove(id)
        return node
    }
    fun drop(id: String) = drop(NavigationNodeId(id))

    fun replace(id: NavigationNodeId, config: Base): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        val i = stack.indexOfFirst { it.id == id }.takeIf { it != -1 } ?: return null

        val newNode = nodeFactory.createNode(this, config) ?: return null
        val oldNode = stack[i]
        _stackFlow.value = _stackFlow.value.take(i) + newNode + _stackFlow.value.drop(i + 1)

        nodesIds.remove(id)
        nodesIds[newNode.id] = newNode

        oldNode.state = NavigationNodeState.NEW

        return oldNode to newNode
    }

    fun replace(
        id: String, config: Base
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
        onFound: NavigationChain<Base>.() -> Unit
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

            (stack.flatMap { it.subchainsFlow.value } + listOfNotNull(parentNode ?.chain)).forEach { chainHolder ->
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
        config: Base,
        visitedNodesChains: MutableSet<NavigationNodeId?>
    ): Boolean = doInTree(id, visitedNodesChains, "replace") {
        replace(id, config)
    }

    fun replaceInTree(
        id: NavigationNodeId,
        config: Base
    ) = replaceInTree(id, config, mutableSetOf())

    fun replaceInTree(
        id: String,
        config: Base
    ) = replaceInTree(NavigationNodeId(id), config)

    private fun pushInTree(
        id: NavigationNodeId,
        config: Base,
        visitedNodesChains: MutableSet<NavigationNodeId?>
    ): Boolean = doInTree(id, visitedNodesChains, "push") {
        push(config)
    }

    fun pushInTree(
        id: NavigationNodeId,
        config: Base
    ) = pushInTree(id, config, mutableSetOf())

    fun pushInTree(
        id: String,
        config: Base
    ) = pushInTree(NavigationNodeId(id), config)

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

        stackFlow.subscribeSafelyWithoutExceptions(subscope) {
            actualizeStackStates()
        }

        val nodeToJob = mutableMapOf<NavigationNodeId, Job>()
        val nodeToJobMutex = Mutex()

        (onNodeAddedFlow + onNodeReplacedFlow.map { it.map { it.second } }).flatten().subscribeSafelyWithoutExceptions(subscope) {
            nodeToJobMutex.withLock {
                nodeToJob[it.value.id] = it.value.start(subscope)
            }
        }
        (onNodeRemovedFlow + onNodeReplacedFlow.map { it.map { it.first } }).flatten().subscribeSafelyWithoutExceptions(subscope) {
            nodeToJobMutex.withLock {
                nodeToJob.remove(it.value.id) ?.cancel()
            }
        }
        onNodeRemovedFlow.dropWhile { stack.isNotEmpty() }.subscribeSafelyWithoutExceptions(subscope) {
            log.d { "Dropping myself from parent node $parentNode" }
            parentNode ?.removeChain(this)
            log.d { "Dropped myself from parent node $parentNode" }
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
