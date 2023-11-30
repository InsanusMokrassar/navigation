package dev.inmo.navigation.core.urls

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo

class UrlsNavigationConfigsRepo<T>(

) : NavigationConfigsRepo<T> {
    override fun save(holder: ConfigHolder<T>) {
        TODO("Not yet implemented")
    }

    override fun get(): ConfigHolder<T>? {
        TODO("Not yet implemented")
    }
}