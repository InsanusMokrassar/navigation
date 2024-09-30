package dev.inmo.navigation.sample.ui.tree

import androidx.compose.runtime.mutableStateOf
import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.onChangesInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.core.visiter.walkOnNodes
import dev.inmo.navigation.mermaid.buildMermaidLines
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CurrentTreeViewViewModel(
    private val node: NavigationNode<CurrentTreeViewConfig, NavigationNodeDefaultConfig>
) : ViewModel<NavigationNodeDefaultConfig>(node) {
    val mermaidLines = mutableStateOf(emptyList<String>())
    private var listeningJob: Job? = null
    private val listeningJobsMutex = Mutex()
    private val changesListeningJob = node.chain.rootChain().onChangesInSubTree(scope) { _, _ ->
        listeningJobsMutex.withLock {
            val flows = mutableListOf<Flow<Any>>(flowOf(Unit))
            node.chain.rootChain().walkOnNodes {
                flows.add(it.statesFlow)
            }
            listeningJob ?.cancel()
            listeningJob = flows.merge().subscribeSafelyWithoutExceptions(scope) {
                updateFun()
            }
        }
    }


    private fun updateFun() {
        val newValue = node.chain.rootChain().buildMermaidLines()

        logger.i { newValue.joinToString("\n") }

        mermaidLines.value = newValue
    }
}