package dev.inmo.navigation.core

import dev.inmo.kslog.common.*
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

    fun replace(
        id: NavigationNodeId,
        config: Base
    ): Pair<NavigationNode<out Base, Base>, NavigationNode<out Base, Base>>? {
        val i = stack.indexOfFirst { it.id == id }.takeIf { it > -1 } ?: return null

        val newNode = nodeFactory.createNode(this, config) ?: return null
        val oldNode = stack[i]
        val currentStack = _stackFlow.value
        _stackFlow.value = currentStack.take(i) + newNode + currentStack.drop(i + 1)

        nodesIds.remove(id)
        nodesIds[newNode.id] = newNode

        oldNode.state = NavigationNodeState.NEW

        return oldNode to newNode
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

    @Deprecated("Extracted to the walking API", ReplaceWith("this.dropInSubTree(id)", "dev.inmo.navigation.core.extensions.dropInSubTree"))
    fun dropInTree(id: NavigationNodeId) = dropInSubTree(id)
    @Deprecated("Extracted to the walking API", ReplaceWith("this.dropNodeInSubTree(id)", "dev.inmo.navigation.core.extensions.dropNodeInSubTree"))
    fun dropInTree(id: String) = dropNodeInSubTree(id)

    @Deprecated("Extracted to the walking API", ReplaceWith("this.replaceInSubTree(id, config)", "dev.inmo.navigation.core.extensions.replaceInSubTree"))
    fun replaceInTree(
        id: NavigationNodeId,
        config: Base
    ) = replaceInSubTree(id, config)

    @Deprecated("Extracted to the walking API", ReplaceWith("this.replaceInSubTree(id, config)", "dev.inmo.navigation.core.extensions.replaceInSubTree"))
    fun replaceInTree(
        id: String,
        config: Base
    ) = replaceInSubTree(id, config)

    @Deprecated("Extracted to the walking API", ReplaceWith("this.pushInSubTree(id, config)", "dev.inmo.navigation.core.extensions.pushInSubTree"))
    fun pushInTree(
        id: NavigationNodeId,
        config: Base
    ) = pushInSubTree(id, config)

    @Deprecated("Extracted to the walking API", ReplaceWith("this.pushInSubTreeByNodeId(id, config)", "dev.inmo.navigation.core.extensions.pushInSubTreeByNodeId"))
    fun pushInTree(
        id: String,
        config: Base
    ) = pushInSubTreeByNodeId(id, config)

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
            parentNode ?.removeSubChain(this)
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
