package dev.inmo.navigation.core

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationNodeState : Comparable<NavigationNodeState> {
    val stateNumber: Byte
    /**
     * State which will be set to [NavigationNode] on its initialization or after its destroying
     */
    @Serializable
    object NEW : NavigationNodeState {
        override val stateNumber: Byte = 0
        override fun toString(): String = "NEW"
    }
    /**
     * State which will be set to [NavigationNode] when it has been created (via constructor) or stopped
     */
    @Serializable
    object CREATED : NavigationNodeState {
        override val stateNumber: Byte = 1
        override fun toString(): String = "CREATED"
    }
    /**
     * State which will be set to [NavigationNode] when it has been started or paused
     */
    @Serializable
    object STARTED : NavigationNodeState {
        override val stateNumber: Byte = 2
        override fun toString(): String = "STARTED"
    }
    /**
     * State which will be set to [NavigationNode] when it has been resumed
     */
    @Serializable
    object RESUMED : NavigationNodeState {
        override val stateNumber: Byte = 3
        override fun toString(): String = "RESUMED"
    }

    override fun compareTo(other: NavigationNodeState): Int = stateNumber.compareTo(other.stateNumber)

    companion object {
        private val values by lazy {
            setOf(NEW, CREATED, STARTED, RESUMED).sorted()
        }
        /**
         * @return All possible [NavigationNodeState]
         */
        fun values() = values
        /**
         * @return [NavigationNodeState] with [stateNumber] if any or null
         */
        fun value(stateNumber: Number): NavigationNodeState? = values.firstOrNull { it.stateNumber == stateNumber.toByte() }
    }
}

/**
 * Next state for current one
 *
 * * For [NavigationNodeState.NEW] it will be [NavigationNodeState.CREATED]
 * * For [NavigationNodeState.CREATED] it will be [NavigationNodeState.STARTED]
 * * For [NavigationNodeState.STARTED] it will be [NavigationNodeState.RESUMED]
 * * For [NavigationNodeState.RESUMED] it will be null
 */
val NavigationNodeState.next: NavigationNodeState?
    get() = NavigationNodeState.value(stateNumber + 1)

/**
 * Previous state for current one
 *
 * * For [NavigationNodeState.NEW] it will be null
 * * For [NavigationNodeState.CREATED] it will be [NavigationNodeState.NEW]
 * * For [NavigationNodeState.STARTED] it will be [NavigationNodeState.CREATED]
 * * For [NavigationNodeState.RESUMED] it will be [NavigationNodeState.STARTED]
 */
val NavigationNodeState.previous: NavigationNodeState?
    get() = NavigationNodeState.value(stateNumber - 1)
