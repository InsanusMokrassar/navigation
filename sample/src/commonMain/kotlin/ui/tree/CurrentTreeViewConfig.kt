package dev.inmo.navigation.sample.ui.tree

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.Serializable

@Serializable
data class CurrentTreeViewConfig(override val id: String) : NavigationNodeDefaultConfig