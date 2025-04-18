package dev.inmo.navigation.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.compose.ComposeView
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class NavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<NavigationViewConfig, NavigationNodeDefaultConfig, NavigationViewModel>(config, chain, id) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    private var animateBack: Boolean = false
    override suspend fun useBeforePauseWait(): Boolean = animateBack

    @Composable
    override fun onDraw() {
        Column {
            Row {
                beforePauseWaitJobState.collectAsState().value ?.let {
                    val leftTime = remember { mutableStateOf(3.seconds) }
                    LaunchedEffect(it) {
                        while (leftTime.value > 0.seconds) {
                            delay(1.seconds)
                            leftTime.value -= 1.seconds
                        }
                        it.complete()
                    }
                    Text(
                        text = "Will be back after ${leftTime.value}",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = config.text, modifier = Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.primary)
                IconButton(
                    {
                        viewModel.back()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("<", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        animateBack = true
                        viewModel.back()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("<A", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createSubChain()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("\\/", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createNextNode(true)
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("+>", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createNextNode(false)
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("->", color = MaterialTheme.colorScheme.primary)
                }
            }
            Column {
                SubchainsHost()
            }
        }
    }
}
