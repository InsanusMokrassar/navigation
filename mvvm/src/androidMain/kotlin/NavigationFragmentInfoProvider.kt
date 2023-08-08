package dev.inmo.navigation.mvvm

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlin.reflect.KClass

interface NavigationFragmentInfoProvider {
    val configsKClasses: Set<KClass<*>>
    fun resolveFragmentKClass(config: NavigationNodeDefaultConfig): KClass<*>?
}
