package dev.inmo.navigation.sample

import org.w3c.dom.HTMLElement
import kotlin.js.json

external interface MermaidContainer {
    fun initialize(config: MermaidContainerConfig = definedExternally)
    fun run(config: MermaidContainerRunConfig = definedExternally)
}
external interface MermaidContainerConfig {
    val startOnLoad: Boolean?
}
fun MermaidContainerConfig(
    startOnLoad: Boolean? = null,
) = json(
    *listOfNotNull(
        startOnLoad ?.let { "start_on_load" to it },
    ).toTypedArray()
).unsafeCast<MermaidContainerConfig>()
external interface MermaidContainerRunConfig {
    val startOnLoad: Boolean?
}
fun MermaidContainerRunConfig(
    querySelector: String? = null,
    nodes: Array<HTMLElement>? = null,
) = json(
    *listOfNotNull(
        querySelector ?.let { "querySelector" to it },
        nodes ?.let { "nodes" to it }
    ).toTypedArray()
).unsafeCast<MermaidContainerRunConfig>()


external val mermaid: MermaidContainer
