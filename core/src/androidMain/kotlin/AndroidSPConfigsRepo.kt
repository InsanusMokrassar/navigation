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
    private val key: String = "navigation",
    private val json: Json
) : NavigationConfigsRepo<T> {
    private val serializer = ConfigHolder.serializer(PolymorphicSerializer<T>(baseConfigKClass))

    constructor(
        sharedPreferences: SharedPreferences,
        baseConfigKClass: KClass<T>,
        configKClasses: List<KClass<out T>>,
        key: String = "navigation",
    ) : this(
        sharedPreferences,
        baseConfigKClass,
        key,
        Json(Json.Default) {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                fun <T : Any> poly(kclass: KClass<T>) {
                    @OptIn(InternalSerializationApi::class)
                    polymorphic(Any::class, kclass, kclass.serializer())
                }
                for (it in configKClasses) {
                    poly(it)
                }
            }
        }
    )

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
                key, json.encodeToString(serializer, holder)
            )
        }.apply()
    }

    override fun get(): ConfigHolder<T>? {
        return sharedPreferences.getString(key, null) ?.let {
            json.decodeFromString(serializer, it)
        }
    }

    companion object {
        inline operator fun <reified T : Any>invoke(
            sharedPreferences: SharedPreferences,
            key: String = "navigation",
            json: Json
        ) = AndroidSPConfigsRepo(sharedPreferences, T::class, key, json)
        inline operator fun <reified T : Any>invoke(
            sharedPreferences: SharedPreferences,
            configKClasses: List<KClass<out T>>,
            key: String = "navigation",
        ) = AndroidSPConfigsRepo(sharedPreferences, T::class, configKClasses, key)
    }
}
