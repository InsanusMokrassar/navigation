package dev.inmo.navigation.core.urls

import dev.inmo.micro_utils.common.Warning
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

@Warning("This feature is unstable and can be complicated. Use with caution")
class UrlParametersNavigationConfigsRepo<T : Any>(
    private val buildSearchParams: LocationData.Builder.(ConfigHolder<T>) -> Unit,
    private val parseSearchParams: (LocationData) -> ConfigHolder<T>?,
    private val titleResolver: (ConfigHolder<T>) -> String? = { null }
) : NavigationConfigsRepo<T> {
    data class LocationData(
        val pathname: String,
        val search: String
    ) {
        val pathSegments by lazy {
            pathname.split("/").toTypedArray()
        }
        val urlSearchParams by lazy {
            URLSearchParams(search)
        }

        constructor(
            pathSegments: Array<String> = emptyArray(),
            urlSearchParams: URLSearchParams = URLSearchParams()
        ) : this(
            pathSegments.joinToString("/"),
            urlSearchParams.toString()
        )

        data class Builder(
            val pathSegments: MutableList<String> = mutableListOf(),
            val parameters: MutableMap<String, MutableList<String>> = mutableMapOf()
        ) {
            fun pathSegment(name: String) = pathSegments.add(name)
            fun parameter(key: String, value: String) = parameters.getOrPut(key) {
                mutableListOf()
            }.add(value)
            fun parameters(key: String, values: Collection<String>) = parameters.getOrPut(key) {
                mutableListOf()
            }.addAll(values)
            fun parameters(key: String, values: Array<String>) = parameters.getOrPut(key) {
                mutableListOf()
            }.addAll(values)

            fun build() = LocationData(
                pathSegments.toTypedArray(),
                URLSearchParams().apply {
                    parameters.forEach { (key, values) ->
                        values.forEach { v ->
                            append(key, v)
                        }
                    }
                }
            )
        }

        fun buildUrl(
            origin: String = document.location ?.origin ?: ""
        ): String {
            val pathname = if (pathSegments.isEmpty()) {
                pathSegments.joinToString("/", prefix = "/")
            } else {
                ""
            }
            val urlQuery = if (search.isEmpty()) {
                ""
            } else {
                "?${urlSearchParams.toString()}"
            }
            return "${origin.removeSuffix("/")}${pathname}${urlQuery}"
        }

        companion object {
            inline fun build(
                initialPathSegments: MutableList<String> = mutableListOf(),
                initialParameters: MutableMap<String, MutableList<String>> = mutableMapOf(),
                block: Builder.() -> Unit
            ): LocationData = Builder(initialPathSegments, initialParameters).apply(block).build()
        }
    }

    override fun save(holder: ConfigHolder<T>) {
        val locationData = LocationData.build { buildSearchParams(holder) }
        window.history.pushState(
            JSON.stringify(holder),
            titleResolver(holder) ?: "",
            locationData.buildUrl()
        )
    }

    override fun get(): ConfigHolder<T>? {
        return parseSearchParams(
            LocationData(
                document.location ?.pathname ?: "",
                document.location ?.search ?: ""
            )
        )
    }
}
