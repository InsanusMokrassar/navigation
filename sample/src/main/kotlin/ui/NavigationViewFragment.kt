package dev.inmo.navigation.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.inmo.navigation.mvvm.NavigationFragmentComposePlace
import dev.inmo.navigation.mvvm.ViewFragment
import dev.inmo.navigation.sample.R
import kotlin.reflect.KClass

class NavigationViewFragment : ViewFragment<NavigationViewModel, NavigationViewConfig>() {
    override val viewModelClass: KClass<NavigationViewModel>
        get() = NavigationViewModel::class

    override val contentBoxModifier: Modifier = Modifier

    @Composable
    override fun BoxScope.Content() {
        Column {
            Row {
                Text(config.text, Modifier.align(Alignment.CenterVertically))
                IconButton(
                    {
                        viewModel.back()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.back))
                }
                IconButton(
                    {
                        viewModel.createSubChain()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.subchain))
                }
                IconButton(
                    {
                        viewModel.createNextNode()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.forward))
                }
            }
            Column {
                viewModel.subnodesIds.forEach {
                    NavigationFragmentComposePlace(it)
                }
            }
        }
    }
}
