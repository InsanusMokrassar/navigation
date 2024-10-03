package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory

val LocalNavigationNodeFactory = compositionLocalOf<NavigationNodeFactory<*>> { NavigationNodeFactory<Any> { _, _ -> null } }

internal val InternalLocalNavigationNodeFactory: ProvidableCompositionLocal<NavigationNodeFactory<*>> = compositionLocalOf<NavigationNodeFactory<*>> { NavigationNodeFactory<Any> { _, _ -> null } }
internal val InternalLocalNavigationChainProvider: ProvidableCompositionLocal<NavigationChain<*>?> = compositionLocalOf<NavigationChain<*>?> { NavigationChain<Any>(null, { _, _ -> null }) }
internal val InternalLocalNavigationNodeProvider: ProvidableCompositionLocal<NavigationNode<*, *>?> = compositionLocalOf {
    null
}

@Suppress("UNCHECKED_CAST")
private fun <Base> LocalNavigationNodeFactory(): ProvidableCompositionLocal<NavigationNodeFactory<Base>> = InternalLocalNavigationNodeFactory as ProvidableCompositionLocal<NavigationNodeFactory<Base>>
@Suppress("UNCHECKED_CAST")
private fun <Base> LocalNavigationChainProvider(): ProvidableCompositionLocal<NavigationChain<Base>?> = InternalLocalNavigationChainProvider as ProvidableCompositionLocal<NavigationChain<Base>?>
@Suppress("UNCHECKED_CAST")
private fun <Base> LocalNavigationNodeProvider(): ProvidableCompositionLocal<NavigationNode<out Base, Base>?> = InternalLocalNavigationNodeProvider as ProvidableCompositionLocal<NavigationNode<out Base, Base>?>

@Composable
fun <Base> getNodeFromLocalProvider(): NavigationNode<out Base, Base>? = LocalNavigationNodeProvider<Base>().current
@Composable
fun <Base> doWithNodeInLocalProvider(node: NavigationNode<out Base, Base>, block: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavigationNodeProvider<Base>() provides node, block)
}

@Composable
fun <Base> getChainFromLocalProvider(): NavigationChain<Base>? = LocalNavigationChainProvider<Base>().current
@Composable
fun <Base> doWithChainInLocalProvider(chain: NavigationChain<Base>, block: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavigationChainProvider<Base>() provides chain, block)
}

@Composable
fun <Base> getNodesFactoryFromLocalProvider(): NavigationNodeFactory<Base> = LocalNavigationNodeFactory<Base>().current
@Composable
fun <Base> doWithNodesFactoryInLocalProvider(factory: NavigationNodeFactory<Base>, block: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavigationNodeFactory<Base>() provides factory, block)
}
