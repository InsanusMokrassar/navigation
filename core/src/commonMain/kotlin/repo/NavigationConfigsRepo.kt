package dev.inmo.navigation.core.repo

interface NavigationConfigsRepo<T> {
    fun save(holder: ConfigHolder<T>)
    fun get(): ConfigHolder<T>?
}
