package dev.inmo.navigation.compose

import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.onChainRemovedFlow
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

/**
 * Collecting [NavigationChain.stackFlow] and always [Draw] only the latest element in [NavigationChain.stackFlow]
 */
@Composable
internal fun <Base> NavigationChain<Base>.DrawStackNodes() {
    val stack = stackFlow.collectAsState()
    key(stack.value.lastOrNull()) {
        val latestNode = stack.value.lastOrNull()
        if (latestNode != null) {
            doWithNodeInLocalProvider(latestNode) {
                when {
                    latestNode is ComposeNode -> {
                        val drawState = latestNode.drawerState.collectAsState()
                        drawState.value ?.invoke()
                    }

                    else -> {
                        latestNode.SubchainsHandling()
                    }
                }
            }
        }
    }
}

/**
 * Calls [Draw] on each [NavigationChain] in [NavigationNode.subchainsFlow] of [this]
 */
@Composable
internal fun <Base> NavigationNode<out Base, Base>.SubchainsHandling(filter: (NavigationChain<Base>) -> Boolean = { true }) {
    val subchainsState = subchainsFlow.collectAsState()
    val rawSubchains = subchainsState.value
    val filteredSubchains = rawSubchains.filter(filter)
    filteredSubchains.forEach {
        it.Draw()
    }
}

/**
 * Main purpose of this function - is to call [DrawStackNodes] to provide stack drawing
 *
 * @param onDismiss Will be called when [this] [NavigationChain] must be dropped
 * @param beforeNodes Will be called **before** [DrawStackNodes] will be called
 */
@Composable
internal fun <Base> NavigationChain<Base>.Draw(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit = {  }
) {
    key(onDismiss, parentNode) {
        onDismiss ?.let {
            parentNode ?.let { parentNode ->
                val scope = rememberCoroutineScope()

                remember {
                    parentNode.onChainRemovedFlow.filter { it.any { it.value === this@Draw } }.subscribeSafelyWithoutExceptions(scope) {
                        onDismiss(this)
                    }
                }
            }
        }
    }

    doWithChainInLocalProvider(this) {
        beforeNodes()
        DrawStackNodes()
    }
}

/**
 * Using [getNodesFactoryFromLocalProvider] to get navigation nodes factory, creating new subchain with
 * [NavigationNode.createEmptySubChain] if [this] [NavigationNode] is not null, and creating new one without parent node
 * with [NavigationChain] constructor. After chain created it will call [Draw] on it
 */
@Composable
internal fun <Base> NavigationNode<*, Base>?.SubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    val factory = getNodesFactoryFromLocalProvider<Base>()
    val chain = remember(this, factory) {
        this ?.createEmptySubChain() ?: NavigationChain<Base>(null, factory)
    }
    chain.Draw(onDismiss, beforeNodes)
}

/**
 * Calling [getNodeFromLocalProvider] and creates [SubChain] on it with passing [onDismiss] and [beforeNodes]
 */
@Composable
fun <Base> InjectNavigationChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    getNodeFromLocalProvider<Base>().SubChain(onDismiss, beforeNodes)
}

/**
 * Just calls [InjectNavigationChain]
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationChain(onDismiss, block)", "dev.inmo.navigation.compose.InjectNavigationChain"))
fun <Base> SubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    InjectNavigationChain(onDismiss, beforeNodes)
}

/**
 * **If [this] is [ComposeNode]** provides [this] with [doWithNodeInLocalProvider] for [actionInContext]
 *
 * @param onDismiss Will be called when [this] [InjectNavigationNode] will be removed from its [NavigationChain]
 */
@Composable
internal fun <Base> NavigationNode<*, Base>.Use(
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    actionInContext: @Composable() (() -> Unit)? = null,
) {
    key(this, actionInContext) {
        doWithNodeInLocalProvider(this) {
            actionInContext ?.invoke()
        }
    }

    key(this, onDismiss) {
        onDismiss ?.let { onDismiss ->
            val scope = rememberCoroutineScope()

            onDestroyFlow.take(1).subscribeSafelyWithoutExceptions(scope) {
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
internal fun <Base> NavigationChain<Base>.NodeInStack(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (() -> Unit)? = null,
) {
    val node: NavigationNode<out Base, Base>? = remember {
        push(config)
    }
    node ?.Use(onDismiss, additionalCodeInNodeContext)
}

/**
 * Trying to get current [NavigationChain] using [getChainFromLocalProvider] and calls [NodeInStack] with
 * passing both [config], [onDismiss] and [additionalCodeInNodeContext]
 *
 * If [NavigationChain] is absent in context, will create new one with [InjectNavigationChain] and pass calling of [NodeInStack]
 * as [NodeInStack]
 */
@Composable
fun <Base> InjectNavigationNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (() -> Unit)? = null,
) {
    val chain = getChainFromLocalProvider<Base>() ?: run {
        InjectNavigationChain<Base> {
            NodeInStack(config, onDismiss, additionalCodeInNodeContext)
        }
        return@InjectNavigationNode
    }
    chain.NodeInStack(config, onDismiss, additionalCodeInNodeContext)
}

/**
 * Just calling [InjectNavigationNode] with passing arguments as is
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationNode(config, onDismiss)", "dev.inmo.navigation.compose.InjectNavigationNode"))
fun <Base> NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (() -> Unit)? = null,
) {
    InjectNavigationNode(config, onDismiss, additionalCodeInNodeContext)
}
