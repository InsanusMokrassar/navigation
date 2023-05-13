package {{.full_module_package}}

import dev.inmo.micro_utils.startup.plugin.createStartupPluginAndRegister

@ExperimentalStdlibApi
@EagerInitialization
@JsExport
@ExperimentalJsExport
private val jsModuleLoader = createStartupPluginAndRegister("{{.module_package}}.JSPlugin") { JSPlugin }
