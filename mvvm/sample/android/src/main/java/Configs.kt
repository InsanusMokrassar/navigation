package dev.inmo.navigation.mvvm.sample.android

import androidx.annotation.IdRes
import kotlinx.serialization.Serializable

@Serializable
sealed interface AndroidNodeConfig {
    val viewTag: String

    @Serializable
    data class TextConfig(
        override val viewTag: String,
        val text: String
    ) : AndroidNodeConfig
}
