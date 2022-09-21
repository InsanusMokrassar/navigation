package dev.inmo.navigation.mvvm.sample.android

import androidx.annotation.IdRes

interface AndroidNodeConfig {
    val viewId: Int

    class TextConfig(
        @IdRes
        override val viewId: Int,
        val text: String
    ) : AndroidNodeConfig
}
