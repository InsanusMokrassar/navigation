package dev.inmo.navigation.sample

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.either
import dev.inmo.navigation.core.*

object NavigationMermaidBuilder {
    internal const val newClassName = "navigation_mermaid_new"
    internal const val createdClassName = "navigation_mermaid_created"
    internal const val resumedClassName = "navigation_mermaid_resumed"
    internal const val startedClassName = "navigation_mermaid_started"
    internal val NavigationNodeState.className
        get() = when (this) {
            NavigationNodeState.NEW -> newClassName
            NavigationNodeState.CREATED -> createdClassName
            NavigationNodeState.STARTED -> startedClassName
            NavigationNodeState.RESUMED -> resumedClassName
        }

    fun className(state: NavigationNodeState) = state.className
}

data class MermaidGraphData(
    val chains: Map<NavigationNode<*, *>?, NavigationChain<*>>,
    val nodes: Map<NavigationChain<*>, List<NavigationNode<*, *>>>,
) {
    operator fun plus(other: MermaidGraphData): MermaidGraphData = MermaidGraphData(
        chains + other.chains,
        nodes + other.nodes,
    )
}

fun NavigationChain<*>.buildMermaidContent(): MermaidGraphData {
    val stack: List<NavigationNode<*, *>> = stackFlow.value
    val data = MermaidGraphData(
        mapOf(parentNode to this),
        mapOf(this to stack)
    )
    val resultData = stack.fold(data) { acc, node ->
        acc + node.buildMermaidContent()
    }
    return resultData
}

fun NavigationNode<*, *>.buildMermaidContent(): MermaidGraphData {
    val capturedSubchains = subchains
    return capturedSubchains.fold(
        MermaidGraphData(emptyMap(), emptyMap())
    ) { acc, node ->
        acc + node.buildMermaidContent()
    }
}

fun Either<NavigationNode<*, *>, NavigationChain<*>>.buildMermaidLines(): List<String> {
    val mermaidGraphData = t1OrNull ?.buildMermaidContent() ?: t2OrNull ?.buildMermaidContent()!!

    val allNodes: Set<NavigationNode<*, *>?> = mermaidGraphData.chains.keys + mermaidGraphData.nodes.values.flatten().distinct()
    val allChains: Set<NavigationChain<*>> = (mermaidGraphData.chains.values + mermaidGraphData.nodes.keys).toSet()

    val nodesDefinitions = allNodes.associate {
        val className = it ?.let { it::class.simpleName }
        it ?.id to "${className}_${it ?.id ?.string}"
    }
    val chainsDefinitions = allChains.associateWith {
        val id = (it.id ?: NavigationChainId())
        id.string
    }

    val links = allNodes.flatMap { node ->
        val definition = nodesDefinitions[node ?.id] ?: return@flatMap emptyList()
        val subchainsLinks = node ?.subchains ?.map {
            val chainDefinition = chainsDefinitions[it]
            "$definition --> $chainDefinition"
        } ?: emptyList()

        val nextNodeId = node ?.chain ?.stackFlow ?.value ?.toList() ?.dropWhile { it != node } ?.drop(1) ?.firstOrNull() ?.id
        val nextNodeDefinition = nextNodeId ?.let {
            nodesDefinitions[it]
        }

        subchainsLinks + listOfNotNull(nextNodeDefinition)
    }

    val resultDefinitions = allNodes.mapNotNull { node ->
        if (mermaidGraphData.nodes.values.any { it.contains(node) }) {
            val className = node ?.let { it::class.simpleName }
            nodesDefinitions[node ?.id] ?.plus("[${className}_${node?.config?.toString()?.map { "#${it.code};" }?.joinToString("")}#91;${node ?.state ?: NavigationNodeState.RESUMED}#93;]")
        } else {
            null
        }
    } + allChains.flatMap {
        val subnodes = mermaidGraphData.nodes[it]
        val definition = chainsDefinitions[it] ?: return@flatMap emptyList()
        var latestNodeDefinition: String? = null
        val subnodesDefinitions = subnodes ?.flatMap {
            val className = it::class.simpleName
            val nodeDefinition = nodesDefinitions[it.id]
            val nodeDefinitionInGraph = nodeDefinition ?.plus("[${className}_${it.config.toString().map { "#${it.code};" }.joinToString("")}#91;${it.state}#93;]")
            listOfNotNull(
                nodeDefinitionInGraph,
                latestNodeDefinition ?.let {
                    "$it ~~~ $nodeDefinition"
                }
            ).also {
                latestNodeDefinition = nodeDefinition
            }
        } ?: emptyList()

        listOf(
            "subgraph $definition[\"Chain\"]",
            "direction TB",
            *subnodesDefinitions.toTypedArray(),
            "end",
        )
    }

    val classnamesLines = allNodes.map {
        val definition = nodesDefinitions[it ?.id]
        val activePausedClassname = when {
            it == null -> NavigationMermaidBuilder.resumedClassName
            else -> NavigationMermaidBuilder.className(it.state)
        }
        "class $definition $activePausedClassname"
    } + allChains.map {
        val activePausedClassname = it.parentNode ?.state ?.let { NavigationMermaidBuilder.className(it) } ?: NavigationMermaidBuilder.resumedClassName
        val definition = chainsDefinitions[it]
        "class $definition $activePausedClassname"
    }

    return resultDefinitions + links + classnamesLines
}

fun NavigationChain<*>.buildMermaidLines(): List<String> {
    return either<NavigationNode<*, *>, NavigationChain<*>>().buildMermaidLines()
}

fun NavigationNode<*, *>.buildMermaidLines(): List<String> {
    return either<NavigationNode<*, *>, NavigationChain<*>>().buildMermaidLines()
}
