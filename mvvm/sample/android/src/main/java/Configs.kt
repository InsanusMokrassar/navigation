package dev.inmo.navigation.mvvm.sample.android

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.Serializable

@Serializable
sealed interface SampleConfig : NavigationNodeDefaultConfig {
    @Serializable
    data class TextConfig(
        override val id: String,
        val text: String
    ) : SampleConfig
}
