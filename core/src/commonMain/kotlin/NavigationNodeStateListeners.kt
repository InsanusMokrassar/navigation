package dev.inmo.navigation.core

import kotlinx.coroutines.flow.filter

val <T> NavigationNode<T>.onCreateFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.CREATE }
val <T> NavigationNode<T>.onStartFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.START }
val <T> NavigationNode<T>.onResumeFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.RESUME }
val <T> NavigationNode<T>.onPauseFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.PAUSE }
val <T> NavigationNode<T>.onStopFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.STOP }
val <T> NavigationNode<T>.onDestroyFlow
    get() = stateChangesFlow.filter { it.type is NavigationStateChange.Type.DESTROY }
