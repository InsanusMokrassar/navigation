package dev.inmo.navigation.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mermaid.NavigationMermaidBuilder
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.navigation.sample.ui.tree.CurrentTreeViewConfig
import dev.inmo.navigation.sample.ui.tree.CurrentTreeViewViewModel
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class CurrentTreeView(
    config: CurrentTreeViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
) : ComposeView<CurrentTreeViewConfig, NavigationNodeDefaultConfig, CurrentTreeViewViewModel>(config, chain) {
    override val viewModel: CurrentTreeViewViewModel by inject {
        parametersOf(this)
    }

    private object CurrentTreeViewStyleSheet : StyleSheet() {
        private fun addClassname(
            classname: String,
            backgroundColor: StylePropertyValue,
            color: StylePropertyValue
        ) {
            className(classname) + "> rect" style {
                variable("${classname}_backgroundColor", "$backgroundColor")
                property("fill", CSSStyleVariable<CSSColorValue>("${classname}_backgroundColor").value().toString() + "!important")
            }
        }
        init {
            addClassname(NavigationMermaidBuilder.newClassName, Color.black, Color.white)
            addClassname(NavigationMermaidBuilder.createdClassName, Color.red, Color.white)
            addClassname(NavigationMermaidBuilder.startedClassName, Color.yellow, Color.white)
            addClassname(NavigationMermaidBuilder.resumedClassName, Color.green, Color.white)
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        key(viewModel.mermaidLines.value) {
            Pre({
                classes("mermaid")
                ref {
                    mermaid.initialize(MermaidContainerConfig(startOnLoad = false))
                    mermaid.run(MermaidContainerRunConfig(nodes = arrayOf(it)))
                    onDispose {

                    }
                }
            }) {
                Text("flowchart TB\n")
                viewModel.mermaidLines.value.forEach {
                    Text("$it\n")
                }
            }
        }
        Style(CurrentTreeViewStyleSheet)
    }
}

