package dev.inmo.navigation.core

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

val NavigationNodeDefaultConfig.htmlElementOrNull
    get() = document.getElementById(id) as? HTMLElement

val NavigationNodeDefaultConfig.htmlElementOrThrow
    get() = htmlElementOrNull ?: error("Unable to find suitable html element")
