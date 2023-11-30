package dev.inmo.navigation.core

import kotlinx.coroutines.flow.filter

val <Base> NavigationNode<out Base, Base>.onCreateFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.CREATE }
val <Base> NavigationNode<out Base, Base>.onStartFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.START }
val <Base> NavigationNode<out Base, Base>.onResumeFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.RESUME }
val <Base> NavigationNode<out Base, Base>.onPauseFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.PAUSE }
val <Base> NavigationNode<out Base, Base>.onStopFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.STOP }
val <Base> NavigationNode<out Base, Base>.onDestroyFlow
    get() = stateChangesFlow.filter { it.type == NavigationStateChange.Type.DESTROY }
