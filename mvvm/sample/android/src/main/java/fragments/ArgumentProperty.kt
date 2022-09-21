package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.fragment.app.Fragment
import kotlin.reflect.KProperty

object ArgumentProperty {
    operator fun <T> getValue(thisRef: Fragment, property: KProperty<*>): T {
        return thisRef.arguments ?.get(property.name) as T
    }
}

fun argument() = ArgumentProperty
