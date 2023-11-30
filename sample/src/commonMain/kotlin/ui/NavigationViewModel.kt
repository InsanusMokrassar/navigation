package dev.inmo.navigation.sample.ui

import androidx.compose.runtime.mutableStateListOf
import com.benasher44.uuid.uuid4
import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.compose.asComposeList
import dev.inmo.micro_utils.coroutines.doInUI
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.onChainAddedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NavigationViewModel (
    private val model: NavigationModel,
    private val node: NavigationNode<NavigationViewConfig, NavigationNodeDefaultConfig>,
) : ViewModel(node) {
    private val _subnodesIds = mutableStateListOf<String>()
    val subnodesIds: List<String> = _subnodesIds

    init {
        node.stateChangesFlow.subscribeSafelyWithoutExceptions(scope) {
            logger.i { "Current state change of node $node is ${it.type}" }
        }
        node.onChainAddedFlow.subscribeSafelyWithoutExceptions(scope) {
            it.forEach {
                it.value.stackFlow.subscribeSafelyWithoutExceptions(scope) {
                    val id = it.firstOrNull() ?.config ?.id ?: return@subscribeSafelyWithoutExceptions
                    doInUI {
                        if (id !in _subnodesIds) {
                            _subnodesIds.add(id)
                        }
                    }
                }
            }
        }
    }

    fun back() {
        node.chain.pop()
    }

    fun createSubChain() {
        val config = node.config

        val maxNumberInSubchains = node.subchains.maxOfOrNull {
            val asNavigationViewConfig = it.stackFlow.value.firstOrNull() ?.config as? NavigationViewConfig ?: return@maxOfOrNull 0
            val text = asNavigationViewConfig.text
            val numberText = text.removePrefix(config.text)
            val number = numberText.toIntOrNull()
            number ?: 0
        } ?: 0

        val id = uuid4().toString()

        node.createSubChain(
            NavigationViewConfig(
                id,
                "${config.text}${maxNumberInSubchains + 1}"
            )
        )
    }

    fun createNextNode() {
        val config = node.config

        node.chain.push(
            config.copy(
                text = ".${config.text}"
            )
        )
    }
}
