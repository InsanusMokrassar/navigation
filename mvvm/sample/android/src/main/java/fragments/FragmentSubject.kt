package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.NavigationNode
import kotlin.reflect.KProperty

abstract class FragmentSubject<Config : Any> : Fragment() {
    protected var node: NavigationNode<Config>? = null
        private set

    open fun configure(node: NavigationNode<Config>) {
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
    }
}
