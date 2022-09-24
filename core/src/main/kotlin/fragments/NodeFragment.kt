package dev.inmo.navigation.core.fragments

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.*
import kotlin.reflect.KProperty

abstract class NodeFragment<Config : Any> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config>
        private set
    protected lateinit var onConfigUpdatedCallback: (Config) -> Unit
    protected val scope = CoroutineScope(Dispatchers.Main)

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    scope.cancel()
                }
            }
        )
    }

    open fun configure(node: AndroidFragmentNode<Config>, onConfigUpdatedCallback: (Config) -> Unit) {
        arguments = bundleOf(
            *node.config::class.members.mapNotNull {
                if (it is KProperty<*>) {
                    it.name to it.getter.call(node.config)
                } else {
                    null
                }
            }.toTypedArray()
        )
        this.node = node
        this.onConfigUpdatedCallback = onConfigUpdatedCallback
    }
}
