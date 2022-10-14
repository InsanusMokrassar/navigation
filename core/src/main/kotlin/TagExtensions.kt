package dev.inmo.navigation.core

import android.view.View

val navigationTagKey = "navigation".hashCode()

var View.navigationTag: Any?
    get() = getTag(navigationTagKey) ?.toString()
    set(value) = setTag(navigationTagKey, value)
