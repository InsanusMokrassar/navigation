package dev.inmo.navigation.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

class NavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<NavigationViewConfig, NavigationNodeDefaultConfig, NavigationViewModel>(config, chain, id) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    @Composable
    override fun onDraw() {
        Column {
            Row {
                Text(config.text, Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.primary)
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
