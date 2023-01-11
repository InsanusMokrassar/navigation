package dev.inmo.navigation.core

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit

abstract class JsNavigationNode<Config : Base, Base : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Base>,
    config: Config,
) : NavigationNode<Config, Base>() {
    protected val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
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
    }

    override fun onPause() {
        super.onPause()
        observer ?.disconnect()
        observer = null
        _htmlElementStateFlow.value = null
    }

}
