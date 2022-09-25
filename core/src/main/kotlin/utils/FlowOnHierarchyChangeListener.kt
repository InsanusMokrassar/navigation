package dev.inmo.navigation.core.utils

import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FlowOnHierarchyChangeListener(
    private val recursive: Boolean = false
) : ViewGroup.OnHierarchyChangeListener {
    private val _onChildViewAdded = MutableSharedFlow<Pair<View, View>>(extraBufferCapacity = Int.MAX_VALUE)
    private val _onChildViewRemoved = MutableSharedFlow<Pair<View, View>>()

    val onChildViewAdded = _onChildViewAdded.asSharedFlow()
    val onChildViewRemoved = _onChildViewRemoved.asSharedFlow()

    override fun onChildViewAdded(parent: View, child: View) {
        _onChildViewAdded.tryEmit(parent to child)

        if (recursive && child is ViewGroup) {
            child.setOnHierarchyChangeListener(this)
        }
    }

    override fun onChildViewRemoved(parent: View, child: View) {
        _onChildViewRemoved.tryEmit(parent to child)
    }
}
