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
 * Collecting [NavigationChain.stackFlow] and always [Draw] only the last element in [NavigationChain.stackFlow]
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

@Composable
fun <Base> NavigationNode<out Base, Base>.SubchainsHandling(filter: (NavigationChain<Base>) -> Boolean = { true }) {
    val subchainsState = subchainsFlow.collectAsState()
    val rawSubchains = subchainsState.value
    val filteredSubchains = rawSubchains.filter(filter)
    filteredSubchains.forEach {
        it.Draw()
    }
}

/**
 * @param onDismiss Will be called when [this] [NavigationChain] will be removed from its parent. [onDismiss] will
 * never be called if [this] [NavigationChain] is the root one
 */
@Composable
internal fun <Base> NavigationChain<Base>.Draw(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit = {  }
) {
    key(onDismiss) {
        onDismiss ?.let {
            key (parentNode) {
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
    }

    doWithChainInLocalProvider(this) {
        beforeNodes()
        DrawStackNodes()
    }
}

/**
 * Creates [NavigationChain] in current composition and call [Draw]
 */
@Composable
internal fun <Base> NavigationNode<*, Base>?.SubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    val factory = LocalNavigationNodeFactory<Base>().current
    val chain = remember(this, factory) {
        this ?.createEmptySubChain() ?: NavigationChain<Base>(null, factory)
    }
    chain.Draw(onDismiss, beforeNodes)
}

/**
 * Trying to get [InjectNavigationNode] using [LocalNavigationNodeProvider] and calling [SubChain] with it
 */
@Composable
fun <Base> InjectNavigationChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    beforeNodes: @Composable NavigationChain<Base>.() -> Unit
) {
    getNodeFromLocalProvider<Base>().SubChain(onDismiss, beforeNodes)
}

/**
 * Trying to get [InjectNavigationNode] using [LocalNavigationNodeProvider] and calling [SubChain] with it
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
 * **If [this] is [ComposeNode]** provides [this] with [LocalNavigationNodeProvider] in [CompositionLocalProvider] and
 * calls [this] [ComposeNode.drawerState] value invoke
 *
 * @param onDismiss Will be called when [this] [InjectNavigationNode] will be removed from its [NavigationChain]
 */
@Composable
internal fun <Base> NavigationNode<*, Base>.Use(
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    actionInContext: @Composable() (() -> Unit)? = null,
) {
    key(this) {
        doWithNodeInLocalProvider(this) {
            actionInContext ?.invoke()
        }
    }

    key(onDismiss) {
        key(this) {
            onDismiss ?.let { onDismiss ->
                this.let { node ->
                    val scope = rememberCoroutineScope()

                    node.onDestroyFlow.take(1).subscribeSafelyWithoutExceptions(scope) {
                        onDismiss(node)
                    }
                }
            }
        }
    }
}

/**
 * Trying to create [InjectNavigationNode] in [this] [NavigationChain] and do [Draw] with passing of [onDismiss] in
 * this call
 */
@Composable
internal fun <Base> NavigationChain<Base>.NodeInStack(
    config: Base,
    additionalCodeInNodeContext: (@Composable () -> Unit)? = null,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val node: NavigationNode<out Base, Base>? = remember {
        push(config)
    }
    node ?.Use(onDismiss, additionalCodeInNodeContext)
}

/**
 * Trying to get current [NavigationChain] using [LocalNavigationChainProvider] and calls [InjectNavigationNode] with
 * passing both [config] and [onDismiss]
 */
@Composable
fun <Base> InjectNavigationNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
    additionalCodeInNodeContext: @Composable() (() -> Unit)? = null,
) {
    val chain = getChainFromLocalProvider<Base>() ?: run {
        InjectNavigationChain<Base> {
            InjectNavigationNode(config, onDismiss, additionalCodeInNodeContext)
        }
        return@InjectNavigationNode
    }
    chain.NodeInStack(config, additionalCodeInNodeContext, onDismiss)
}

/**
 * Trying to get current [NavigationChain] using [LocalNavigationChainProvider] and calls [InjectNavigationNode] with
 * passing both [config] and [onDismiss]
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
