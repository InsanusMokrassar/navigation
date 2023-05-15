package dev.inmo.navigation.mvvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.jetbrains.compose.web.renderComposable
import org.koin.core.component.KoinComponent

/**
 * Javascript realization of View for MVVM. Uses [Config] as a type of config it retrieves on creation and
 * [VM] as a type of [ViewModel].
 *
 * In case you are using DI, you should create some view factory to allocate realization of [View] based on incoming
 * config
 */
abstract class View<Config: NavigationNodeDefaultConfig, VM: ViewModel> (
    config: Config,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    override val id: NavigationNodeId = NavigationNodeId()
) : JsNavigationNode<Config, NavigationNodeDefaultConfig>(chain, config), KoinComponent {
    protected fun initComposition(): Composition = renderComposable(htmlElementOrThrow) {}
    protected var composition: Composition? = null
        private set
    protected abstract val viewModel: VM
    protected val scope: CoroutineScope
        get() = viewModel.scope

    protected var compositionJob: Job? = null

    @Composable
    protected open fun onDraw() {}

    override fun onStart() {
        super.onStart()

        compositionJob = htmlElementStateFlow.subscribeSafelyWithoutExceptions(scope) {
            if (it == null) {
                composition ?.dispose()
                composition = null
            } else {
                composition = initComposition()

                if (state == NavigationNodeState.RESUMED) {
                    composition ?.setContent { onDraw() }
                } else {
                    composition ?.setContent {  }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        composition ?.setContent { onDraw() }
    }

    override fun onPause() {
        super.onPause()

        composition ?.setContent {}
    }

    override fun onStop() {
        super.onStop()
        composition ?.dispose()
        composition = null
        compositionJob ?.cancel()
    }

    override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }
}
