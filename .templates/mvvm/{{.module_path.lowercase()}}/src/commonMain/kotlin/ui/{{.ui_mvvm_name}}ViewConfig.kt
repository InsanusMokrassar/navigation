package {{.full_module_package}}.ui

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.serialization.Serializable

@Serializable
data class {{.ui_mvvm_name}}ViewConfig(
    override val id: String
) : NavigationNodeDefaultConfig
