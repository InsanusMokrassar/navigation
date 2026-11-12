package dev.inmo.navigation.core.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.flow.MutableStateFlow

abstract class NodeFragment<Config : Base, Base : NavigationNodeDefaultConfig> : Fragment() {
    @Volatile
    private var _node: AndroidFragmentNode<Config, Base>? = null
    protected val Log by lazy {
        logger
    }
    internal val callbacks: CallbacksCollection by lazy {
        CallbacksCollection.Callbacks(
            onNavigationCreateCallback = { onNavigationCreate() },
            onNavigationStartCallback = { onNavigationStart() },
            onNavigationResumeCallback = { onNavigationResume() },
            onNavigationPauseCallback = { onNavigationPause() },
            onNavigationStopCallback = { onNavigationStop() },
            onNavigationDestroyCallback = { onNavigationDestroy() },
            onBeforeNavigationCreateCallback = { onBeforeNavigationCreate() },
            onBeforeNavigationStartCallback = { onBeforeNavigationStart() },
            onBeforeNavigationResumeCallback = { onBeforeNavigationResume() },
            onBeforeNavigationPauseCallback = { onBeforeNavigationPause() },
            onBeforeNavigationStopCallback = { onBeforeNavigationStop() },
            onBeforeNavigationDestroyCallback = { onBeforeNavigationDestroy() },
        )
    }

    internal val hasInactiveAncestor: Boolean
        get() {
            var ancestor = _node?.chain?.parentNode
            while (ancestor != null) {
                if (ancestor.state < NavigationNodeState.RESUMED) {
                    return true
                }
                ancestor = ancestor.chain.parentNode
            }
            return false
        }

    // The ancestor's transition already animates its subtree. Starting a separate
    // transition in a departing child container can detach it before its first
    // pre-draw, leaving FragmentManager waiting for an end callback that never runs.
    override fun getEnterTransition(): Any? = if (hasInactiveAncestor) null else super.getEnterTransition()
    override fun getExitTransition(): Any? = if (hasInactiveAncestor) null else super.getExitTransition()
    override fun getReenterTransition(): Any? = if (hasInactiveAncestor) null else super.getReenterTransition()
    override fun getReturnTransition(): Any? = if (hasInactiveAncestor) null else super.getReturnTransition()
    override fun getSharedElementEnterTransition(): Any? = if (hasInactiveAncestor) null else super.getSharedElementEnterTransition()
    override fun getSharedElementReturnTransition(): Any? = if (hasInactiveAncestor) null else super.getSharedElementReturnTransition()

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        var ancestor = _node?.chain?.parentNode
        while (ancestor != null) {
            (ancestor as? AndroidFragmentNode<*, *>)?.onDescendantViewCreated(this, viewLifecycleOwner.lifecycle)
            ancestor = ancestor.chain.parentNode
        }
    }

    override fun onDestroyView() {
        var ancestor = _node?.chain?.parentNode
        while (ancestor != null) {
            (ancestor as? AndroidFragmentNode<*, *>)?.onDescendantViewDestroyed(this)
            ancestor = ancestor.chain.parentNode
        }
        super.onDestroyView()
    }

    /** Available after navigation binds this Fragment, before its ordinary lifecycle callbacks. */
    protected var node: AndroidFragmentNode<Config, Base>
        get() = checkNotNull(_node) {
            "NodeFragment.node is unavailable before navigation binds the Fragment. " +
                "Activities using initNavigation() must call prepareNavigationRestore() before super.onCreate(savedInstanceState)."
        }
        private set(value) {
            _node = value
        }
    @Volatile
    private var __configState: MutableStateFlow<Config>? = null
    protected var _configState: MutableStateFlow<Config>
        get() = checkNotNull(__configState) {
            "NodeFragment.config is unavailable before navigation binds the Fragment. " +
                "Activities using initNavigation() must call prepareNavigationRestore() before super.onCreate(savedInstanceState)."
        }
        set(value) {
            __configState = value
        }
    protected var config: Config
        get() = _configState.value
        set(value) {
            _configState.value = value
        }

    protected open fun onSetNode(
        node: AndroidFragmentNode<Config, Base>,
        configStateFlow: MutableStateFlow<Config>
    ) {

    }

    internal fun setNode(
        node: AndroidFragmentNode<Config, Base>,
        configStateFlow: MutableStateFlow<Config>
    ) {
        this.node = node
        this._configState = configStateFlow
        onSetNode(node, configStateFlow)
    }

    open fun onNavigationCreate() {
        Log.d { "onCreate" }
    }
    open fun onNavigationStart() {
        Log.d { "onStart" }
    }
    open fun onNavigationResume() {
        Log.d { "onResume" }
    }
    open fun onNavigationPause() {
        Log.d { "onPause" }
    }
    open fun onNavigationStop() {
        Log.d { "onStop" }
    }
    open fun onNavigationDestroy() {
        Log.d { "onDestroy" }
    }

    open suspend fun onBeforeNavigationCreate() {
        Log.d { "onBeforeCreate" }
    }
    open suspend fun onBeforeNavigationStart() {
        Log.d { "onBeforeStart" }
    }
    open suspend fun onBeforeNavigationResume() {
        Log.d { "onBeforeResume" }
    }
    open suspend fun onBeforeNavigationPause() {
        Log.d { "onBeforePause" }
    }
    open suspend fun onBeforeNavigationStop() {
        Log.d { "onBeforeStop" }
    }
    open suspend fun onBeforeNavigationDestroy() {
        Log.d { "onBeforeDestroy" }
    }
}
