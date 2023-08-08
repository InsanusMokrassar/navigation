package dev.inmo.navigation.core

import dev.inmo.navigation.core.fragments.NodeFragment
import kotlin.reflect.KClass

typealias FragmentsClassesFactory<T> = (T) -> KClass<*>?
