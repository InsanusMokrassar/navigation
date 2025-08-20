package dev.inmo.navigation.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.inmo.micro_utils.coroutines.Default
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

fun main(args: Array<String>) {
    CoroutineScope(Default).launch {
        StartLauncherPlugin.start(
            Config(
                listOf(
                    JVMPlugin
                )
            )
        )
    }

    return application {
        Window(onCloseRequest = ::exitApplication) {
            MaterialTheme {
                JVMPlugin.currentDrawingBlock.collectAsState().value.invoke()
            }
        }
    }
}