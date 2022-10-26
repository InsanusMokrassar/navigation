package dev.inmo.navigation.core

import android.view.View
import dev.inmo.micro_utils.common.findViewsByTag

val navigationTagKey = "navigation".hashCode()

var View.navigationTag: Any?
    get() = getTag(navigationTagKey) ?.toString()
    set(value) = setTag(navigationTagKey, value)

fun View.findViewsWithNavigationTag(tag: Any?) = findViewsByTag(this, navigationTagKey, tag)
