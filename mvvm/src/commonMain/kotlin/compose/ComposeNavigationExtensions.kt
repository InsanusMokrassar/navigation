package dev.inmo.navigation.mvvm.compose

import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.onNodeRemovedFlow
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.take

@Composable
fun <Base> NavigationNode<*, Base>.NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    val chain = remember { createEmptySubChain() }

    key(onDismiss) {
        onDismiss ?.let {
            val scope = rememberCoroutineScope()

            remember {
                chain.onNodeRemovedFlow().dropWhile { chain.stackFlow.value.isNotEmpty() }.subscribeSafelyWithoutExceptions(scope) {
                    onDismiss(chain)
                }
            }
        }
    }

    CompositionLocalProvider(LocalNavigationChainProvider<Base>() provides chain) {
        chain.block()
    }
    DisposableEffect(chain) {
        onDispose {
            chain.clear()
            chain.dropItself()
        }
    }
}

@Composable
fun <Base> NavigationSubChain(
    onDismiss: (suspend NavigationChain<Base>.() -> Unit)? = null,
    block: @Composable NavigationChain<Base>.() -> Unit
) {
    LocalNavigationNodeProvider<Base>().current.NavigationSubChain(onDismiss, block)
}

@Composable
fun <Base> NavigationChain<Base>.NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    val node: NavigationNode<out Base, Base>? = remember { push(config) }
    val nodeAsComposeView = derivedStateOf { node as? ComposeView }

    key(nodeAsComposeView.value) {
        nodeAsComposeView.value ?.let { view ->
            CompositionLocalProvider(LocalNavigationNodeProvider<Base>() provides view) {
                view.drawerState.collectAsState().value ?.invoke()
            }
        }
    }

    key(onDismiss) {
        key(node) {
            onDismiss ?.let { onDismiss ->
                node ?.let { node ->
                    val scope = rememberCoroutineScope()

                    node.onDestroyFlow.take(1).subscribeSafelyWithoutExceptions(scope) {
                        onDismiss(node)
                    }
                }
            }
        }
    }

    DisposableEffect(node) {
        onDispose {
            node ?.let {
                this@NavigationSubNode.drop(it)
            }
        }
    }
}

@Composable
fun <Base> NavigationSubNode(
    config: Base,
    onDismiss: (suspend NavigationNode<*, Base>.() -> Unit)? = null,
) {
    LocalNavigationChainProvider<Base>().current.NavigationSubNode(config, onDismiss)
}
