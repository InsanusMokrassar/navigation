package dev.inmo.navigation.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.abs

@Serializable
@JvmInline
value class NavigationStateChange internal constructor(
    val pair: Pair<NavigationNodeState, NavigationNodeState>
) {
    @Serializable
    sealed interface Type {
        @Serializable
        object CREATE : Type
        @Serializable
        object START : Type
        @Serializable
        object RESUME : Type
        @Serializable
        object PAUSE : Type
        @Serializable
        object STOP : Type
        @Serializable
        object DESTROY : Type
    }
    val from: NavigationNodeState
        get() = pair.first
    val to: NavigationNodeState
        get() = pair.second

    val isPositive
        get() = to.stateNumber > from.stateNumber

    val isNegative
        get() = !isPositive

    private val onCreate
        get() = to == NavigationNodeState.CREATED && from == NavigationNodeState.NEW
    private val onStop
        get() = to == NavigationNodeState.CREATED && from != NavigationNodeState.NEW

    private val onDestroy
        get() = to == NavigationNodeState.NEW && from != NavigationNodeState.NEW

    private val onPause
        get() = to == NavigationNodeState.STARTED && from == NavigationNodeState.RESUMED
    private val onStart
        get() = to == NavigationNodeState.STARTED && from == NavigationNodeState.CREATED

    private val onResume
        get() = to == NavigationNodeState.RESUMED

    val type: Type
        get() = when {
            onCreate -> Type.CREATE
            onStart -> Type.START
            onResume -> Type.RESUME
            onPause -> Type.PAUSE
            onStop -> Type.STOP
            onDestroy -> Type.DESTROY
            else -> Type.CREATE
        }

    init {
        require(from != to) {
            "Change should contains different states, but get $pair"
        }
        require(abs(from.stateNumber - to.stateNumber) == 1) {
            "Change should be created from previous to the next step, but got $pair"
        }
    }
}

fun NavigationStateChange(
    old: NavigationNodeState,
    new: NavigationNodeState
): NavigationStateChange? {
    return if (old == new) {
        null
    } else {
        NavigationStateChange(old to new)
    }
}

fun NavigationStateChangeList(
    old: NavigationNodeState,
    new: NavigationNodeState
): List<NavigationStateChange> {
    val states = when {
        old.stateNumber < new.stateNumber -> {
            (old.stateNumber .. new.stateNumber).mapNotNull {
                NavigationNodeState.value(it)
            }
        }
        old.stateNumber > new.stateNumber -> {
            (old.stateNumber downTo new.stateNumber).mapNotNull {
                NavigationNodeState.value(it)
            }
        }
        else -> emptyList()
    }

    return (1 until states.size).mapNotNull {
        NavigationStateChange(
            states[it - 1],
            states[it]
        )
    }
}



/**
 * Change from current state to the [NavigationNodeState.next]
 */
val NavigationNodeState.nextChange: NavigationStateChange?
    get() {
        return NavigationStateChange(this to (next ?: return null))
    }

/**
 * Change from current state to the [NavigationNodeState.previous]
 */
val NavigationNodeState.previousChange: NavigationStateChange?
    get() {
        return NavigationStateChange(this to (previous ?: return null))
    }
