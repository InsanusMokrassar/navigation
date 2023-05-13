package dev.inmo.navigation.sample

import dev.inmo.micro_utils.startup.plugin.createStartupPluginAndRegister

@ExperimentalStdlibApi
@EagerInitialization
@JsExport
@ExperimentalJsExport
private val jsModuleLoader = createStartupPluginAndRegister("navigation.sample.JSPlugin") { JSPlugin }
