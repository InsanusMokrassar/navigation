package dev.inmo.navigation.mvvm.sample.android.repo

import android.content.SharedPreferences
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import kotlinx.serialization.json.Json

class AndroidSPConfigsRepo(
    private val sharedPreferences: SharedPreferences
) : NavigationConfigsRepo<AndroidNodeConfig> {
    private val json: Json = Json.Default
    private val serializer = ConfigHolder.serializer(AndroidNodeConfig.serializer())
    override fun save(holder: ConfigHolder<AndroidNodeConfig>) {
        sharedPreferences.edit().apply {
            putString(
                "navigation", json.encodeToString(serializer, holder)
            )
        }.apply()
    }

    override fun get(): ConfigHolder<AndroidNodeConfig>? {
        return sharedPreferences.getString("navigation", null) ?.let {
            json.decodeFromString(serializer, it)
        }
    }
}
