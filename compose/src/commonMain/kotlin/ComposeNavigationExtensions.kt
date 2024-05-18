package dev.inmo.navigation.compose

import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.onNodeRemovedFlow
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.take

@Composable
internal fun <Base> NavigationChain<Base>.defaultStackHandling() {
    val stack = stackFlow.collectAsState()
    stack.value.lastOrNull() ?.StartInCompose()
}

@Composable
internal fun <Base> NavigationChain<Base>.StartInCompose(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit = { defaultStackHandling() }
) {
    key(onDismiss) {
        onDismiss ?.let {
            val scope = rememberCoroutineScope()

            remember {
                onNodeRemovedFlow().dropWhile { stackFlow.value.isNotEmpty() }.subscribeSafelyWithoutExceptions(scope) {
                    onDismiss(this)
                }
            }
        }
    }

    CompositionLocalProvider(LocalNavigationChainProvider<Base>() provides this) {
        block()
    }
    DisposableEffect(this) {
        onDispose {
            clear()
            dropItself()
        }
    }
}

@Composable
fun <Base> NavigationNode<*, Base>.NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    remember { createEmptySubChain() }.StartInCompose(onDismiss, block)
}

@Composable
fun <Base> NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    LocalNavigationNodeProvider<Base>().current.NavigationSubChain(onDismiss, block)
}

@Composable
internal fun <Base> NavigationNode<*, Base>?.StartInCompose(
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val nodeAsComposeNode = derivedStateOf { this as? ComposeNode }

    key(nodeAsComposeNode.value) {
        nodeAsComposeNode.value ?.let { view ->
            CompositionLocalProvider(LocalNavigationNodeProvider<Base>() provides view) {
                view.drawerState.collectAsState().value ?.invoke()
            }
        }
    }

    key(onDismiss) {
        key(this) {
            onDismiss ?.let { onDismiss ->
                this ?.let { node ->
                    val scope = rememberCoroutineScope()

                    node.onDestroyFlow.take(1).subscribeSafelyWithoutExceptions(scope) {
                        onDismiss(node)
                    }
                }
            }
        }
    }

    DisposableEffect(this) {
        onDispose {
            this@StartInCompose ?.let {
                this@StartInCompose.chain.drop(this@StartInCompose)
            }
        }
    }
}

@Composable
fun <Base> NavigationChain<Base>.NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val node: NavigationNode<out Base, Base>? = remember { push(config) }
    node.StartInCompose(onDismiss)
}

@Composable
fun <Base> NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    LocalNavigationChainProvider<Base>().current.NavigationSubNode(config, onDismiss)
}
