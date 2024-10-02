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
 * Collecting [NavigationChain.stackFlow] and always [StartInCompose] only the last element in [NavigationChain.stackFlow]
 */
@Composable
internal fun <Base> NavigationChain<Base>.defaultStackHandling() {
    val stack = stackFlow.collectAsState()
    key(stack.value.lastOrNull()) {
        val latestNode = stack.value.lastOrNull()
        if (latestNode != null) {
            CompositionLocalProvider(LocalNavigationNodeProvider<Base>() provides latestNode) {
                when {
                    latestNode is ComposeNode -> {
                        val drawState = latestNode.drawerState.collectAsState()
                        drawState.value ?.invoke()
                    }

                    else -> {
                        latestNode.defaultSubchainsHandling()
                    }
                }
            }
        }
    }
}

@Composable
fun <Base> NavigationNode<out Base, Base>.defaultSubchainsHandling(filter: (NavigationChain<Base>) -> Boolean = { true }) {
    val subchainsState = subchainsFlow.collectAsState()
    val rawSubchains = subchainsState.value
    val filteredSubchains = rawSubchains.filter(filter)
    filteredSubchains.forEach {
        it.StartInCompose()
    }
}

/**
 * @param onDismiss Will be called when [this] [NavigationChain] will be removed from its parent. [onDismiss] will
 * never be called if [this] [NavigationChain] is the root one
 */
@Composable
internal fun <Base> NavigationChain<Base>.StartInCompose(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit = {  }
) {
    key(onDismiss) {
        onDismiss ?.let {
            key (parentNode) {
                parentNode ?.let { parentNode ->
                    val scope = rememberCoroutineScope()

                    remember {
                        parentNode.onChainRemovedFlow.filter { it.any { it.value === this@StartInCompose } }.subscribeSafelyWithoutExceptions(scope) {
                            onDismiss(this)
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalNavigationChainProvider<Base>() provides this) {
        block()
        defaultStackHandling()
    }
}

/**
 * Creates [NavigationChain] in current composition and call [StartInCompose]
 */
@Composable
fun <Base> NavigationNode<*, Base>?.NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    val factory = LocalNavigationNodeFactory<Base>().current
    val chain = remember(this, factory) {
        this ?.createEmptySubChain() ?: NavigationChain<Base>(null, factory)
    }
    chain.StartInCompose(onDismiss, block)
}

/**
 * Trying to get [InjectNavigationNode] using [LocalNavigationNodeProvider] and calling [NavigationSubChain] with it
 */
@Composable
fun <Base> InjectNavigationChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    LocalNavigationNodeProvider<Base>().current.NavigationSubChain(onDismiss, block)
}

/**
 * Trying to get [InjectNavigationNode] using [LocalNavigationNodeProvider] and calling [NavigationSubChain] with it
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationChain(onDismiss, block)", "dev.inmo.navigation.compose.InjectNavigationChain"))
fun <Base> NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    InjectNavigationChain(onDismiss, block)
}

/**
 * **If [this] is [ComposeNode]** provides [this] with [LocalNavigationNodeProvider] in [CompositionLocalProvider] and
 * calls [this] [ComposeNode.drawerState] value invoke
 *
 * @param onDismiss Will be called when [this] [InjectNavigationNode] will be removed from its [NavigationChain]
 */
@Composable
internal fun <Base> NavigationNode<*, Base>.StartInCompose(
    additionalCodeInNodeContext: (@Composable () -> Unit)? = null,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    key(this) {
        CompositionLocalProvider(LocalNavigationNodeProvider<Base>() provides this) {
            additionalCodeInNodeContext ?.invoke()
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
 * Trying to create [InjectNavigationNode] in [this] [NavigationChain] and do [StartInCompose] with passing of [onDismiss] in
 * this call
 */
@Composable
fun <Base> NavigationChain<Base>.NavigationSubNode(
    config: Base,
    additionalCodeInNodeContext: (@Composable () -> Unit)? = null,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val node: NavigationNode<out Base, Base>? = remember {
        push(config)
    }
    node ?.StartInCompose(additionalCodeInNodeContext, onDismiss)
}

/**
 * Trying to get current [NavigationChain] using [LocalNavigationChainProvider] and calls [NavigationSubNode] with
 * passing both [config] and [onDismiss]
 */
@Composable
fun <Base> InjectNavigationNode(
    config: Base,
    additionalCodeInNodeContext: (@Composable () -> Unit)? = null,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val chain = LocalNavigationChainProvider<Base>().current ?: run {
        InjectNavigationChain<Base> {
            InjectNavigationNode(config, additionalCodeInNodeContext, onDismiss)
        }
        return@InjectNavigationNode
    }
    chain.NavigationSubNode(config, additionalCodeInNodeContext, onDismiss)
}

/**
 * Trying to get current [NavigationChain] using [LocalNavigationChainProvider] and calls [NavigationSubNode] with
 * passing both [config] and [onDismiss]
 */
@Composable
@Deprecated("Renamed", ReplaceWith("InjectNavigationNode(config, onDismiss)", "dev.inmo.navigation.compose.InjectNavigationNode"))
fun <Base> NavigationSubNode(
    config: Base,
    additionalCodeInNodeContext: (@Composable () -> Unit)? = null,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    InjectNavigationNode(config, additionalCodeInNodeContext, onDismiss)
}
