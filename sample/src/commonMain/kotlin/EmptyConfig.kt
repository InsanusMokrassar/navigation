package dev.inmo.navigation.sample

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.Serializable

@Serializable
data class EmptyConfig(
    override val id: String
) : NavigationNodeDefaultConfig