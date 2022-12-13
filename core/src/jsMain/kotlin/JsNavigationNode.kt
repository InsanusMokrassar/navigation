package dev.inmo.navigation.core

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.MutationObserver

abstract class JsNavigationNode<Config : Base, Base : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Base>,
    config: Config,
) : NavigationNode<Config, Base>() {
    protected val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
    protected val htmlElementOrThrow
        get() = configState.value.htmlElementOrThrow
    override var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }

    protected var observer: MutationObserver? = null

    override fun onResume() {
        runCatching {
            htmlElementOrThrow
        }.onSuccess {
            super.onResume()
        }.onFailure {
            observer = MutationObserver { _, mutationObserver ->
                runCatching {
                    htmlElementOrThrow
                }.onSuccess {
                    if (state == NavigationNodeState.RESUMED) {
                        super.onResume()
                    }
                    mutationObserver.disconnect()
                }
            }.apply {
                observe(document.body ?: return)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        observer ?.disconnect()
        observer = null
    }

}
