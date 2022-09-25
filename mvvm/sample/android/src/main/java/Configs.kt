package dev.inmo.navigation.mvvm.sample.android

import dev.inmo.navigation.core.AndroidNodeConfig
import kotlinx.serialization.Serializable

@Serializable
sealed interface SampleConfig : AndroidNodeConfig {
    @Serializable
    data class TextConfig(
        override val viewTag: String,
        val text: String
    ) : SampleConfig
}
