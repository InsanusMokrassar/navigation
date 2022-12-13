package dev.inmo.navigation.core

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.browser.localStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.w3c.dom.get

class CookiesNavigationConfigsRepo<T>(
    private val json: Json,
    private val serializer: KSerializer<ConfigHolder<T>>,
    private val key: String = "navigation"
) : NavigationConfigsRepo<T> {
    override fun save(holder: ConfigHolder<T>) {
        val serialized = json.encodeToString(serializer, holder)
        localStorage.setItem(key, serialized)
    }

    override fun get(): ConfigHolder<T>? {
        return json.decodeFromString(
            serializer,
            localStorage.getItem(key) ?: return null
        )
    }
}
