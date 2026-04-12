package dev.inmo.navigation.compose

import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.onChainRemovedFlow
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

@Composable
internal fun <Base> DrawNode(node: NavigationNode<out Base, Base>) {
    when {
        node is ComposeNode -> {
            val drawState = node.drawerState.collectAsState()
            drawState.value ?.invoke()
        }

        else -> {
            SubchainsHandling<Base>()
        }
    }
}
/**
 * Collecting [NavigationChain.stackFlow] and always [DrawChain] only the latest element in [NavigationChain.stackFlow]
 */
@Composable
internal fun <Base> DrawStackNodes() {
    val chain = getChainFromLocalProvider<Base>() ?: return
    val stack = chain.stackFlow.collectAsState()
    val latestNode = remember(stack.value.lastOrNull()) {
        stack.value.lastOrNull()
    }
    val provider = InternalLocalComposeInjectedChainsAndNodesProvider.current
    key(latestNode, latestNode ?.hashCode()) {
        if (latestNode != null) {
            doWithNodeInLocalProvider(latestNode) {
                if (latestNode !in provider.nodes) {
                    DrawNode(latestNode)
                }
            }
        }
    }
}

/**
 * Calls [DrawChain] on each [NavigationChain] in [NavigationNode.subchainsFlow] of [this]
 */
@Composable
internal fun <Base> SubchainsHandling(filter: (NavigationChain<Base>) -> Boolean = { true }) {
    val node = getNodeFromLocalProvider<Base>() ?: return
    val subchainsState = node.subchainsFlow.collectAsState()
    val rawSubchains = subchainsState.value
    val filteredSubchains = rawSubchains.filter(filter)

    val provider = InternalLocalComposeInjectedChainsAndNodesProvider.current
    filteredSubchains.forEach {
        if (provider.chains.contains(it) == false) {
            doWithChainInLocalProvider(it) {
                DrawChain<Base>()
            }
        }
    }
}

/**
 * Main purpose of this function - is to call [DrawStackNodes] to provide stack drawing
 *
 * @param onDismiss Will be called when [this] [NavigationChain] must be dropped
 * @param beforeNodes Will be called **before** [DrawStackNodes] will be called
 */
@Composable
internal fun <Base> DrawChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit = {  }
) {
    val chain = getChainFromLocalProvider<Base>() ?: return
    key(onDismiss, chain.parentNode) {
        onDismiss ?.let {
            chain.parentNode ?.let { parentNode ->
                val scope = rememberCoroutineScope()

                remember {
                    parentNode.onChainRemovedFlow.filter { it.any { it.value === chain } }.subscribeLoggingDropExceptions(scope) {
                        onDismiss(chain)
                    }
                }
            }
        }
    }

    chain.beforeNodes()
    DrawStackNodes<Base>()
}

/**
 * Calling [getNodeFromLocalProvider] and creates [SubChain] on it with passing [onDismiss] and [beforeNodes]
 */
@Composable
fun <Base> InjectNavigationChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    id: NavigationChainId? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    val rootNode = getNodeFromLocalProvider<Base>()
    val factory = getNodesFactoryFromLocalProvider<Base>()
    val chain = remember(rootNode, factory) {
        if (rootNode == null) {
            NavigationChain<Base>(parentNode = null, nodeFactory = factory, id = id)
        } else {
            rootNode.createEmptySubChain(id = id)
        }
    }
    val provider = InternalLocalComposeInjectedChainsAndNodesProvider.current
    remember(chain) { provider.chains.add(chain) }
    DisposableEffect(provider, chain) {
        onDispose {
            provider.chains.remove(chain)
        }
    }
    doWithChainInLocalProvider(chain) {
        DrawChain(onDismiss, beforeNodes)
    }
}

/**
 * Just calls [InjectNavigationChain]
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationChain(onDismiss, id, block)", "dev.inmo.navigation.compose.InjectNavigationChain"))
fun <Base> SubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    id: NavigationChainId? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    InjectNavigationChain(onDismiss = onDismiss, id = id, beforeNodes = beforeNodes)
}

/**
 * **If [this] is [ComposeNode]** provides [this] with [doWithNodeInLocalProvider] for [actionInContext]
 *
 * @param onDismiss Will be called when [this] [InjectNavigationNode] will be removed from its [NavigationChain]
 */
@Composable
internal fun <Base> NavigationNode<*, Base>.Use(
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    actionInContext: @Composable() (NavigationNode<out Base, Base>.() -> Unit)? = null,
) {
    key(this, actionInContext) {
        doWithNodeInLocalProvider(this) {
            actionInContext ?.invoke(this)
        }
    }

    key(this, onDismiss) {
        onDismiss ?.let { onDismiss ->
            val scope = rememberCoroutineScope()

            onDestroyFlow.take(1).subscribeLoggingDropExceptions(scope) {
                onDismiss(this)
            }
        }
    }
}

/**
 * Calls [NavigationChain.push] on [this] receiver and [Use] returned [NavigationNode] with passing of
 * [onDismiss] and [additionalCodeInNodeContext] to it
 */
@Composable
internal fun <Base> PushAndDrawNodeInStack(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (NavigationNode<out Base, Base>.() -> Unit)? = null,
) {
    val chain = getChainFromLocalProvider<Base>() ?: return
    val node: MutableState<NavigationNode<out Base, Base>?> = remember { mutableStateOf(null) }
    LaunchedEffect(config) {
        val currentValue = node.value
        node.value = chain.push(config)
        if (currentValue != null) {
            chain.drop(currentValue)
        }
    }
    val provider = InternalLocalComposeInjectedChainsAndNodesProvider.current
    node.value ?.let {
        remember { provider.nodes.add(it) }
        DisposableEffect(provider, it) {
            onDispose {
                provider.nodes.remove(it)
            }
        }
    }
    node.value ?.Use(onDismiss, additionalCodeInNodeContext)

    node.value ?.let {
        DrawNode(it)
    }
}

/**
 * Trying to get current [NavigationChain] using [getChainFromLocalProvider] and calls [PushAndDrawNodeInStack] with
 * passing both [config], [onDismiss] and [additionalCodeInNodeContext]
 *
 * If [NavigationChain] is absent in context, will create new one with [InjectNavigationChain] and pass calling of [PushAndDrawNodeInStack]
 * as [PushAndDrawNodeInStack]
 */
@Composable
fun <Base> InjectNavigationNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (NavigationNode<out Base, Base>.() -> Unit)? = null,
) {
    val existsChain = getChainFromLocalProvider<Base>()
    if (existsChain == null) {
        InjectNavigationChain<Base> {
            PushAndDrawNodeInStack(config, onDismiss, additionalCodeInNodeContext)
        }
    } else {
        PushAndDrawNodeInStack(config, onDismiss, additionalCodeInNodeContext)
    }
}

/**
 * Just calling [InjectNavigationNode] with passing arguments as is
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationNode(config, onDismiss, additionalCodeInNodeContext)", "dev.inmo.navigation.compose.InjectNavigationNode"))
fun <Base> NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (NavigationNode<out Base, Base>.() -> Unit)? = null,
) {
    InjectNavigationNode(config, onDismiss, additionalCodeInNodeContext)
}
