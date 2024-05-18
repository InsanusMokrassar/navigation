package dev.inmo.navigation.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory

val LocalNavigationNodeFactory = compositionLocalOf<NavigationNodeFactory<*>> { NavigationNodeFactory<Any> { _, _ -> null } }

internal val InternalLocalNavigationChainProvider: ProvidableCompositionLocal<NavigationChain<*>> = compositionLocalOf<NavigationChain<*>> { NavigationChain<Any>(null, { _, _ -> null }) }
internal val InternalLocalNavigationNodeProvider: ProvidableCompositionLocal<NavigationNode<*, *>> = compositionLocalOf {
    NavigationNode.Empty<Any, Any>(
        NavigationChain(null, { _, _ -> null}),
        Unit
    )
}

internal fun <Base> LocalNavigationChainProvider(): ProvidableCompositionLocal<NavigationChain<Base>> = InternalLocalNavigationChainProvider as ProvidableCompositionLocal<NavigationChain<Base>>
internal fun <Base> LocalNavigationNodeProvider(): ProvidableCompositionLocal<NavigationNode<out Base, Base>> = InternalLocalNavigationNodeProvider as ProvidableCompositionLocal<NavigationNode<out Base, Base>>
