package dev.inmo.navigation.core.configs

interface NavigationNodeDefaultConfig {
    val id: String
    val storableInNavigationHierarchy: Boolean
        get() = true
}
