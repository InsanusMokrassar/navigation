package dev.inmo.navigation.core.urls

import dev.inmo.kslog.common.taggedLogger
import dev.inmo.kslog.common.w
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.browser.window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import org.w3c.dom.url.URLSearchParams

/**
 * @param encodeDecoded Will be used when new navigation tree must be sent into navigation param.
 * As a fact, the result of this callback will be inserted into url param
 * @param decodeEncoded Will be used when repo will get value of navigation param to prepare it for
 * [format]
 */
class OneParameterUrlNavigationConfigsRepo<T : Any>(
    private val format: StringFormat,
    private val configSerializer: KSerializer<T>,
    private val queryParam: String = "navigation",
    private val encodeDecoded: (String) -> String = { it },
    private val decodeEncoded: (String) -> String = { it },
) : NavigationConfigsRepo<T> {
    private val logger = taggedLogger(this)
    private val serializer = ConfigHolder.serializer(configSerializer)
    private val search: () -> URLSearchParams = {
        URLSearchParams(window.location.search)
    }

    override fun save(holder: ConfigHolder<T>) {
        val baseSearchQuery = search()
        val stringToReplace = baseSearchQuery.toString()
        val newEncodedState = encodeDecoded(format.encodeToString(serializer, holder))
        baseSearchQuery.set(queryParam, newEncodedState)
        val newQuery = baseSearchQuery.toString()
        val newUrl = if (stringToReplace.isEmpty()) {
            "${window.location.href}?${newQuery}"
        } else {
            window.location.href.replace(stringToReplace, newQuery)
        }
        window.history.pushState(
            newEncodedState,
            queryParam,
            newUrl
        )
    }

    override fun get(): ConfigHolder<T>? {
        return runCatching {
            format.decodeFromString(serializer, search().get(queryParam) ?.let(decodeEncoded) ?: return null)
        }.onFailure {
            logger.w(it) { "Unable to load navigation by param $queryParam from url" }
        }.getOrNull()
    }
}
