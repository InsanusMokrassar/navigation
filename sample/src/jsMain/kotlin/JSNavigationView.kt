package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.navigation.sample.ui.NavigationViewConfig
import dev.inmo.navigation.sample.ui.NavigationViewModel
import org.jetbrains.compose.web.dom.*
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class JSNavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<NavigationViewConfig, NavigationNodeDefaultConfig, NavigationViewModel>(config, chain, id) {
    override val viewModel: NavigationViewModel by inject {
        parametersOf(this)
    }

    @Composable
    override fun onDraw() {
        when {
            chain.parentNode == null && chain.stackFlow.collectAsState().value.first() === this@JSNavigationView -> Pre({
                classes("mermaid")
                ref {
                    mermaid.initialize(MermaidContainerConfig(startOnLoad = false))
                    mermaid.run(MermaidContainerRunConfig(nodes = arrayOf(it)))
                    onDispose {

                    }
                }
            }) {
                if (chain.parentNode == null) {
                    Text("flowchart TB\n")
                }
                Text("${config.id}[${config.text}]\n")

                SubchainsHost()
            }

        }
//        if (chain.parentNode == null) {
//
//        } else {
//
//        }
//
//        Div({
//            style {
//                display(DisplayStyle.Flex)
//                flexWrap(FlexWrap.Nowrap)
//            }
//        }) {
//            Span { Text(config.text) }
//            Button({
//                onClick { viewModel.back() }
//            }) {
//                Text("<")
//            }
//            Button({
//                onClick { viewModel.createSubChain() }
//            }) {
//                Text("\\/")
//            }
//            Button({
//                onClick { viewModel.createNextNode(true) }
//            }) {
//                Text("+>")
//            }
//            Button({
//                onClick { viewModel.createNextNode(false) }
//            }) {
//                Text("->")
//            }
//        }
//        Div({
//            style {
//                paddingLeft(8.px)
//            }
//        }) {
//            SubchainsHost()
//        }
    }
}
