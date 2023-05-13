package {{.full_module_package}}.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import dev.inmo.navigation.mvvm.ViewFragment
import {{.full_module_package}}.ui.{{.ui_mvvm_name}}ViewConfig
import {{.full_module_package}}.ui.{{.ui_mvvm_name}}ViewModel
import kotlin.reflect.KClass

class {{.ui_mvvm_name}}ViewFragment : ViewFragment<{{.ui_mvvm_name}}ViewModel, {{.ui_mvvm_name}}ViewConfig>() {
    override val viewModelClass: KClass<{{.ui_mvvm_name}}ViewModel>
        get() = {{.ui_mvvm_name}}ViewModel::class

    @Composable
    override fun BoxScope.Content() {
    }
}
