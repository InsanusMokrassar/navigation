package dev.inmo.navigation.mvvm.sample.android

import androidx.annotation.IdRes
import kotlinx.serialization.Serializable

interface AndroidNodeConfig {
    val viewId: Int

    @Serializable
    data class TextConfig(
        @IdRes
        override val viewId: Int,
        val text: String
    ) : AndroidNodeConfig
}
