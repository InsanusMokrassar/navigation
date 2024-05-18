package dev.inmo.navigation.compose

import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import org.koin.core.Koin


fun <Base> Koin.nodeFactory(): NavigationNodeFactory<Base> {
    val factories = getAllDistinct<NavigationNodeFactory<Base>>()
    return NavigationNodeFactory<Base> { chainHolder, config ->
        factories.firstNotNullOfOrNull { it.createNode(chainHolder, config) }
    }.also {
        TagLogger("nodeFactory").d { "Navigation node factory inited with next factories: $factories" }
    }
}
