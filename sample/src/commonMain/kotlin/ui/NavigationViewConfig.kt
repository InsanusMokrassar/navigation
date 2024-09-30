package dev.inmo.navigation.sample.ui

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.Serializable

@Serializable
data class NavigationViewConfig(
    override val id: String,
    val text: String,
    override val storableInNavigationHierarchy: Boolean = true
) : NavigationNodeDefaultConfig {
    override fun toString(): String {
        return text
    }
}
