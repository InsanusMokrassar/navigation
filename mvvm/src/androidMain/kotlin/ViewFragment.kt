package dev.inmo.navigation.mvvm

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.compose.asComposeState
import dev.inmo.micro_utils.koin.lazyInject
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.AndroidFragmentNode
import dev.inmo.navigation.core.fragments.NodeFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

/**
 * Android variant of View in MVVM architecture. It is receiving [ViewModel] and [Config] as required types
 * for [NodeFragment]
 *
 * There are several important things to view in context of this abstract class:
 *
 * * [backIsPop]
 * * [Log]
 * * [viewModelClass]
 * * [contentBoxModifier]
 * * [SetContent]
 *
 * This class handling the lifecycle of this view automatically: it will create android [View] in [onCreateView],
 * set the [Content] there and destroy when the view or lifecycle is destroying
 */
abstract class ViewFragment<ViewModel: dev.inmo.navigation.mvvm.ViewModel<NavigationNodeDefaultConfig>, Config : NavigationNodeDefaultConfig> : NodeFragment<Config, NavigationNodeDefaultConfig>() {
    /**
     * If true, [onBackPressedCallback] will be registered to handle the back presses and current [node] will be
     * popped from its [dev.inmo.navigation.core.NavigationChain] on back press
     */
    protected open var backIsPop: Boolean = false
        set(value) {
            if (value) {
                if (isResumed) {
                    registerBackListener()
                }
            } else {
                unregisterBackListener()
            }
            field = value
        }
    protected val Log by lazy {
        logger
    }

    /**
     * This class of [ViewModel] will be used to create [viewModel] with [lazyInject]
     */
    protected abstract val viewModelClass: KClass<ViewModel>
    protected val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    val viewModel: ViewModel by lazyInject(kClassFactory = ::viewModelClass) {
        Log.d { "Retrieving view model" }
        parametersOf(node)
    }

    protected val scope: CoroutineScope
        get() = viewModel.scope

    protected val configState: State<Config> by lazy {
        _configState.asComposeState(scope)
    }

//    protected val composeStyles: ComposeStyles
//        get() = currentStyles()

    protected fun inflateComposeViewAndDraw(
        container: ViewGroup?
    ): ComposeView? {
        return container ?.let {
            val view = ComposeView(container.context)
            view
        }
    }

    protected open val contentBoxModifier: Modifier = Modifier.fillMaxSize()

    /**
     * Draw your content of current view here. It will be called in the [Box] with modifier = [contentBoxModifier]
     * in the [contentDrawer] which will be used in the [SetContent]
     */
    @Composable
    protected abstract fun BoxScope.Content()

    private val contentDrawer: @Composable () -> Unit = {
        Log.d { "Draw root content Box" }
        Box(modifier = contentBoxModifier) {
            Log.d { "Draw content in box" }
            Content()
        }
    }

    protected open fun onBackPressed() {
        if (backIsPop && node.state == NavigationNodeState.RESUMED) {
            node.chain.drop(node.id)
        }
    }

    /**
     * Will be called when the content is set to the view
     */
    @Composable
    protected open fun SetContent() {
        contentDrawer()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d { "Set view" }
        return view ?: inflateComposeViewAndDraw(container) ?.apply {
            Log.d { "Set content to view" }
            setContent {
                SetContent()
            }
        }
    }

    private fun registerBackListener() {
        activity ?.onBackPressedDispatcher ?.addCallback(onBackPressedCallback)
    }

    private fun unregisterBackListener() {
        onBackPressedCallback.remove()
    }

    override fun onResume() {
        super.onResume()
        if (backIsPop) {
            registerBackListener()
        }
    }

    override fun onPause() {
        super.onPause()
        if (backIsPop) {
            unregisterBackListener()
        }
    }

    protected override fun onSetNode(
        node: AndroidFragmentNode<Config, NavigationNodeDefaultConfig>,
        configStateFlow: MutableStateFlow<Config>
    ) {
        super.onSetNode(node, configStateFlow)
        viewModel // just init view model
    }
}
