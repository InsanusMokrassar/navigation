package dev.inmo.navigation.core

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.browser.localStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import org.w3c.dom.Storage

class StorageNavigationConfigsRepo<T>(
    private val format: StringFormat,
    private val serializer: KSerializer<ConfigHolder<T>>,
    private val key: String = "navigation",
    private val storage: Storage = localStorage
) : NavigationConfigsRepo<T> {
    override fun save(holder: ConfigHolder<T>) {
        val serialized = format.encodeToString(serializer, holder)
        storage.setItem(key, serialized)
    }

    override fun get(): ConfigHolder<T>? {
        return format.decodeFromString(
            serializer,
            storage.getItem(key) ?: return null
        )
    }
}

@Deprecated("Renamed", ReplaceWith("StorageNavigationConfigsRepo<T>", "dev.inmo.navigation.core.StorageNavigationConfigsRepo"))
typealias CookiesNavigationConfigsRepo<T> = StorageNavigationConfigsRepo<T>
