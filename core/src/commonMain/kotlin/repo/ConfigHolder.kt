package dev.inmo.navigation.core.repo

import kotlinx.serialization.Serializable

sealed interface ConfigHolder<T> {
    @Serializable
    data class Simple<T>(
        val config: T,
        val subconfig: ConfigHolder<T>?
    ) : ConfigHolder<T>
    @Serializable
    data class Parent<T>(
        val configs: List<Simple<T>>
    ) : ConfigHolder<T>
}
