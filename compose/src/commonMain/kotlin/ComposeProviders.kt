package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory

val LocalNavigationNodeFactory = compositionLocalOf<NavigationNodeFactory<*>> { NavigationNodeFactory<Any> { _, _ -> null } }

internal val InternalLocalNavigationNodeFactory: ProvidableCompositionLocal<NavigationNodeFactory<*>> = compositionLocalOf<NavigationNodeFactory<*>> { NavigationNodeFactory<Any> { _, _ -> null } }
internal val InternalLocalNavigationChainProvider: ProvidableCompositionLocal<NavigationChain<*>> = compositionLocalOf<NavigationChain<*>> { NavigationChain<Any>(null, { _, _ -> null }) }
internal val InternalLocalNavigationNodeProvider: ProvidableCompositionLocal<NavigationNode<*, *>> = compositionLocalOf {
    NavigationNode.Empty<Any, Any>(
        NavigationChain(null, { _, _ -> null}),
        Unit
    )
}

@Suppress("UNCHECKED_CAST")
internal fun <Base> InternalLocalNavigationNodeFactory(): ProvidableCompositionLocal<NavigationNodeFactory<Base>> = InternalLocalNavigationNodeFactory as ProvidableCompositionLocal<NavigationNodeFactory<Base>>
@Suppress("UNCHECKED_CAST")
internal fun <Base> LocalNavigationChainProvider(): ProvidableCompositionLocal<NavigationChain<Base>> = InternalLocalNavigationChainProvider as ProvidableCompositionLocal<NavigationChain<Base>>
@Suppress("UNCHECKED_CAST")
internal fun <Base> LocalNavigationNodeProvider(): ProvidableCompositionLocal<NavigationNode<out Base, Base>> = InternalLocalNavigationNodeProvider as ProvidableCompositionLocal<NavigationNode<out Base, Base>>
