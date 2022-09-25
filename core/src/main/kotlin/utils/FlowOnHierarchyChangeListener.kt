package dev.inmo.navigation.core.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FlowOnHierarchyChangeListener(
    private val recursive: Boolean = false
) : ViewGroup.OnHierarchyChangeListener {
    private val _onChildViewAdded = MutableSharedFlow<Pair<View, View>>(extraBufferCapacity = Int.MAX_VALUE)
    private val _onChildViewRemoved = MutableSharedFlow<Pair<View, View>>()

    val onChildViewAdded = _onChildViewAdded.asSharedFlow()
    val onChildViewRemoved = _onChildViewRemoved.asSharedFlow()

    constructor(recursive: Boolean = false, recursiveRoot: ViewGroup) : this(recursive) {
        fun subscribeRecursively(viewGroup: ViewGroup) {
            viewGroup.setOnHierarchyChangeListener(this)
            viewGroup.children.forEach {
                if (it is ViewGroup) {
                    subscribeRecursively(it)
                }
            }
        }
        subscribeRecursively(recursiveRoot)
    }

    override fun onChildViewAdded(parent: View, child: View) {
        logger.d { "Added: ${parent to child}" }
        _onChildViewAdded.tryEmit(parent to child)

        if (recursive && child is ViewGroup) {
            child.setOnHierarchyChangeListener(this)
        }
    }

    override fun onChildViewRemoved(parent: View, child: View) {
        logger.d { "Removed: ${parent to child}" }
        _onChildViewRemoved.tryEmit(parent to child)
    }
}
