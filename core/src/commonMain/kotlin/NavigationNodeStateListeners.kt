package dev.inmo.navigation.core

import kotlinx.coroutines.flow.filter

val <Base> NavigationNode<out Base, Base>.onCreateFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.CREATE }
val <Base> NavigationNode<out Base, Base>.onStartFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.START }
val <Base> NavigationNode<out Base, Base>.onResumeFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.RESUME }
val <Base> NavigationNode<out Base, Base>.onPauseFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.PAUSE }
val <Base> NavigationNode<out Base, Base>.onStopFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.STOP }
val <Base> NavigationNode<out Base, Base>.onDestroyFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.DESTROY }
