package dev.inmo.navigation.sample.ui


import androidx.compose.runtime.Composable
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.View
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class NavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>
) : View<NavigationViewConfig, NavigationViewModel>(config, chain) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        Div {
            Div({
                style {
                    display(DisplayStyle.InlineBlock)
                }
            }) {
                Text(config.text)
                Button(
                    {
                        onClick {
                            viewModel.back()
                        }
                    }
                ) {
                    Text("<")
                }
                Button(
                    {
                        onClick {
                            viewModel.createSubChain()
                        }
                    }
                ) {
                    Text("\\/")
                }
                Button(
                    {
                        onClick {
                            viewModel.createNextNode()
                        }
                    },
                ) {
                    Text(">")
                }
            }
            Div {
                for (it in viewModel.subnodesIds) {
                    Div({ id(it) })
                }
            }
        }
    }
}
