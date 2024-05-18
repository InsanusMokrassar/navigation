package dev.inmo.navigation.mvvm

import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.koin.lazyInject
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass


abstract class JvmComposeView<Config: Base, Base, VM: ViewModel<Base>>(
    config: Config,
    chain: NavigationChain<Base>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<Config, Base, VM>(config, chain, id) {
    protected abstract val viewModelClass: KClass<VM>
    override val viewModel: VM by lazyInject(kClassFactory = ::viewModelClass) {
        log.d { "Retrieving view model" }
        parametersOf(this)
    }
}