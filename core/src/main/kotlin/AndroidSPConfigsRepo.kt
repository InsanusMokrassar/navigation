package dev.inmo.navigation.core

import android.content.SharedPreferences
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass

class AndroidSPConfigsRepo<T: Any>(
    private val sharedPreferences: SharedPreferences,
    baseConfigKClass: KClass<T>,
    configKClasses: List<KClass<out T>>,
) : NavigationConfigsRepo<T> {
    private val json: Json = Json(Json.Default) {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            fun <T : Any> poly(kclass: KClass<T>) {
                @OptIn(InternalSerializationApi::class)
                polymorphic(Any::class, kclass, kclass.serializer())
            }
            configKClasses.forEach {
                poly(it)
            }
        }
    }
    private val serializer = ConfigHolder.serializer(PolymorphicSerializer<T>(baseConfigKClass))

    constructor(
        sharedPreferences: SharedPreferences,
        baseConfigKClass: KClass<T>,
        subclass: KClass<out T>,
        vararg subclasses: KClass<out T>
    ) : this(sharedPreferences, baseConfigKClass, listOf(subclass) + subclasses.toList())

    constructor(
        sharedPreferences: SharedPreferences,
        baseConfigKClass: KClass<T>
    ) : this(sharedPreferences, baseConfigKClass, baseConfigKClass.sealedSubclasses)

    override fun save(holder: ConfigHolder<T>) {
        sharedPreferences.edit().apply {
            putString(
                "navigation", json.encodeToString(serializer, holder)
            )
        }.apply()
    }

    override fun get(): ConfigHolder<T>? {
        return sharedPreferences.getString("navigation", null) ?.let {
            json.decodeFromString(serializer, it)
        }
    }
}
