package dev.inmo.navigation.sample.ui

import android.os.Bundle
import android.transition.TransitionInflater
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

    override val contentBoxModifier: Modifier
        get() = if (node.chain.parentNode == null) Modifier.safeDrawingPadding() else Modifier

    @Composable
    override fun SetContent() {
        MaterialTheme {
            super.SetContent()
        }
    }

    @Composable
    override fun BoxScope.Content() {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(configState.value.text, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = viewModel::back) {
                    Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = viewModel::createSubChain) {
                    Text(stringResource(R.string.subchain), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.createNextNode(true) }) {
                    Text(stringResource(R.string.forward), color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.createNextNode(false) }) {
                    Text(stringResource(R.string.forwardUnstorable), color = MaterialTheme.colorScheme.primary)
                }
            }

            val subchains by node.subchainsFlow.collectAsState()
            for (chain in subchains) {
                key(chain) {
                    val stack by chain.stackFlow.collectAsState()
                    stack.firstOrNull()?.config?.id?.let { id ->
                        key(id) {
                            NavigationFragmentComposePlace(id)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }
}
