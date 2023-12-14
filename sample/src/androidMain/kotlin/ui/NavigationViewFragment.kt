package dev.inmo.navigation.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.inmo.navigation.core.NavigationNodeState
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
                Text(config.text, Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.primary)
                IconButton(
                    {
                        viewModel.back()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createSubChain()
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.subchain), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createNextNode(true)
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.forward), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    {
                        viewModel.createNextNode(false)
                    },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.forwardUnstorable), color = MaterialTheme.colorScheme.primary)
                }
            }
            Column {
                for (it in viewModel.subnodesIds) {
                    NavigationFragmentComposePlace(it)
                }
            }
        }
    }
}
