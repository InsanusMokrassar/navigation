package dev.inmo.navigation.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.mvvm.JvmComposeView
import dev.inmo.navigation.sample.R
import kotlin.reflect.KClass

class NavigationView(
    config: NavigationViewConfig,
    chain: NavigationChain<NavigationNodeDefaultConfig>,
    id: NavigationNodeId = NavigationNodeId()
) : JvmComposeView<NavigationViewConfig, NavigationNodeDefaultConfig, NavigationViewModel>(config, chain, id) {
    override val viewModelClass: KClass<NavigationViewModel>
        get() = NavigationViewModel::class

    @Composable
    override fun onDraw() {
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
                SubchainsHost()
            }
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val inflater = TransitionInflater.from(requireContext())
//        enterTransition = inflater.inflateTransition(R.transition.slide_right)
//        exitTransition = inflater.inflateTransition(R.transition.fade)
//    }
}
