package dev.inmo.navigation.core

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class NavigationChainId(
    val string: String = uuid4().toString()
)
