package dev.inmo.navigation.mvvm

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.onDestroyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class ViewModel(
    node: NavigationNode<out NavigationNodeDefaultConfig, NavigationNodeDefaultConfig>
) {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        node.onDestroyFlow.subscribeSafelyWithoutExceptions(scope) {
            scope.cancel()
        }
    }
}
