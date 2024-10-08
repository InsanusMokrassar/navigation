package dev.inmo.navigation.sample

import dev.inmo.micro_utils.coroutines.Default
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun main() {
    window.addEventListener("load", {
        CoroutineScope(Default).launch {
            CommonPlugin.useDefaultNavigationViewFactory = false // disabling default navigation view in favor to js implementation
            StartLauncherPlugin.start(
                Config(
                    listOf(JSPlugin)
                )
            )
        }
    })
}