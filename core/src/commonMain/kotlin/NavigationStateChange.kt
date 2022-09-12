package dev.inmo.navigation.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.abs

@Serializable
@JvmInline
value class NavigationStateChange internal constructor(
    val pair: Pair<NavigationNodeState, NavigationNodeState>
) {
    val from: NavigationNodeState
        get() = pair.first
    val to: NavigationNodeState
        get() = pair.second

    val isPositive
        get() = from.stateNumber >= to.stateNumber

    val isNegative
        get() = !isPositive

    val onCreate
        get() = to == NavigationNodeState.CREATED && from == NavigationNodeState.NEW
    val onStop
        get() = to == NavigationNodeState.CREATED && from != NavigationNodeState.NEW

    val onDestroy
        get() = to == NavigationNodeState.NEW && from != NavigationNodeState.NEW

    val onPause
        get() = to == NavigationNodeState.STARTED && from == NavigationNodeState.RESUMED
    val onStart
        get() = to == NavigationNodeState.STARTED && from == NavigationNodeState.CREATED

    val onResume
        get() = to == NavigationNodeState.RESUMED

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
