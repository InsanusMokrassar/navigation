package dev.inmo.navigation.mvvm.compose

import androidx.compose.runtime.Composition
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.navigation.compose.ComposeNode
import dev.inmo.navigation.core.*
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.koin.core.component.KoinComponent

/**
 * Javascript realization of View for MVVM. Uses [Config] as a type of config it retrieves on creation and
 * [VM] as a type of [ViewModel].
 *
 * In case you are using DI, you should create some view factory to allocate realization of [ComposeView] based on incoming
 * config
 */
abstract class ComposeView<Config: Base, Base, VM: ViewModel<Base>> (
    config: Config,
    chain: NavigationChain<Base>,
    override val id: NavigationNodeId = NavigationNodeId()
) : ComposeNode<Config, Base>(config, chain), KoinComponent {
    protected abstract val viewModel: VM
    protected val scope: CoroutineScope
        get() = viewModel.scope

    override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }

    override fun onCreate() {
        super.onCreate()
        viewModel.onCreate()
    }
    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }
    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override suspend fun onBeforeCreate() {
        super.onBeforeCreate()
        viewModel.onBeforeCreate()
    }
    override suspend fun onBeforeStart() {
        super.onBeforeStart()
        viewModel.onBeforeStart()
    }
    override suspend fun onBeforeResume() {
        super.onBeforeResume()
        viewModel.onBeforeResume()
    }
    override suspend fun onBeforePause() {
        super.onBeforePause()
        viewModel.onBeforePause()
    }
    override suspend fun onBeforeStop() {
        super.onBeforeStop()
        viewModel.onBeforeStop()
    }
    override suspend fun onBeforeDestroy() {
        super.onBeforeDestroy()
        viewModel.onBeforeDestroy()
    }
}
