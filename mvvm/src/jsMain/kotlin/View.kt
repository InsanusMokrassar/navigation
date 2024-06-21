package dev.inmo.navigation.mvvm

import androidx.compose.runtime.Composition
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.compose.ComposeView
import kotlinx.browser.document
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.web.renderComposable
import org.koin.core.component.KoinComponent
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit

/**
 * Javascript realization of View for MVVM. Uses [Config] as a type of config it retrieves on creation and
 * [VM] as a type of [ViewModel].
 *
 * In case you are using DI, you should create some view factory to allocate realization of [View] based on incoming
 * config
 */
abstract class HtmlView<Config: NavigationNodeDefaultConfig, VM: ViewModel<NavigationNodeDefaultConfig>> (
    config: Config,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : ComposeView<Config, NavigationNodeDefaultConfig, VM>(config, chain, id), KoinComponent {
    protected fun initComposition(): Composition = renderComposable(htmlElementOrThrow) {}
    protected var composition: Composition? = null
        private set

    protected var compositionJob: Job? = null
    protected val htmlElementOrThrow
        get() = configState.value.htmlElementOrThrow
    private val _htmlElementStateFlow = MutableStateFlow<HTMLElement?>(null)
    protected val htmlElementStateFlow = _htmlElementStateFlow.asStateFlow()

    override var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }

    protected var observer: MutationObserver? = null

    override fun onResume() {
        super.onResume()
        inline fun refresh() {
            _htmlElementStateFlow.value = runCatching {
                htmlElementOrThrow
            }.getOrNull()
        }
        observer = MutationObserver { _, _ ->
            refresh()
        }.apply {
            observe(document, MutationObserverInit(childList = true, subtree = true, attributes = true))
        }
        refresh()

        composition ?.setContent { onDraw() }
    }

    override fun onPause() {
        super.onPause()
        observer ?.disconnect()
        observer = null
        _htmlElementStateFlow.value = null
        composition ?.setContent {}
    }

    override fun onCreate() {
        super.onCreate()
        viewModel // just init viewModel
    }

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

typealias View<Config, VM> = HtmlView<Config, VM>
