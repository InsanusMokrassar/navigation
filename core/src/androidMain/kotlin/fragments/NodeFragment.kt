package dev.inmo.navigation.core.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.flow.MutableStateFlow

abstract class NodeFragment<Config : Base, Base : NavigationNodeDefaultConfig> : Fragment() {
    private var _node: AndroidFragmentNode<Config, Base>? = null

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

    private var _nodeSyncObject: Object? = Object()
    protected var node: AndroidFragmentNode<Config, Base>
        get() = _nodeSyncObject ?.let {
            synchronized(it) {
                while (_node == null && _nodeSyncObject != null) {
                    it.wait()
                }
                _node!!
            }
        } ?: _node!!
        private set(value) {
            _nodeSyncObject ?.let {
                synchronized(it) {
                    _node = value
                    it.notifyAll()
                    _nodeSyncObject = null
                }
            } ?: let {
                _node = value
            }
        }
    private var __configState: MutableStateFlow<Config>? = null
    private var _configStateSyncObject: Object? = Object()
    protected var _configState: MutableStateFlow<Config>
        get() = _configStateSyncObject ?.let {
            synchronized(it) {
                while (__configState == null && _configStateSyncObject != null) {
                    it.wait()
                }
                __configState!!
            }
        } ?: __configState!!
        set(value) {
            _configStateSyncObject ?.let {
                synchronized(it) {
                    __configState = value
                    it.notifyAll()
                    _configStateSyncObject = null
                }
            } ?: let {
                __configState = value
            }
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
}
