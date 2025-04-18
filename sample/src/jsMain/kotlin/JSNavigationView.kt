package dev.inmo.navigation.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds

class JSNavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<NavigationViewConfig, NavigationNodeDefaultConfig, NavigationViewModel>(config, chain, id) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    private var animateBack: Boolean = false
    private val animationTime = 3.seconds
    override suspend fun useBeforePauseWait(): Boolean = animateBack

    @Composable
    override fun onDraw() {
        val animationState = remember { mutableStateOf<Float?>(null) }
        Div({
            style {
                display(DisplayStyle.Flex)
                flexWrap(FlexWrap.Nowrap)
                animationState.value ?.let {
                    opacity(it)
                }
            }
        }) {
            beforePauseWaitJobState.collectAsState().value ?.let {
                val leftTime = remember { mutableStateOf(animationTime) }
                LaunchedEffect(it) {
                    val tick = 1f / (leftTime.value.inWholeSeconds + 1).toFloat()
                    animationState.value = 1f
                    while (leftTime.value > 0.seconds) {
                        animationState.value = animationState.value ?.minus(tick)
                        delay(1.seconds)
                        leftTime.value -= 1.seconds
                    }
                    it.complete()
                }

                Span { Text("Will be back after ${leftTime.value}") }
            }
            Span { Text(config.text) }
            Button({
                onClick { viewModel.back() }
            }) {
                Text("<")
            }
            Button({
                onClick {
                    animateBack = true
                    viewModel.back()
                }
            }) {
                Text("<A")
            }
            Button({
                onClick { viewModel.createSubChain() }
            }) {
                Text("\\/")
            }
            Button({
                onClick { viewModel.createNextNode(true) }
            }) {
                Text("+>")
            }
            Button({
                onClick { viewModel.createNextNode(false) }
            }) {
                Text("->")
            }
        }
        Div({
            style {
                paddingLeft(8.px)
            }
        }) {
            SubchainsHost()
        }
    }
}
