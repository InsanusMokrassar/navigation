package dev.inmo.navigation.mvvm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.NoOpUpdate
import dev.inmo.navigation.core.fragments.view.NavigationFragmentContainerView
import dev.inmo.navigation.core.navigationTag

/**
 * Creates [AndroidView] with [NavigationFragmentContainerView]
 *
 * @param id Target tag of [NavigationFragmentContainerView]
 */
@Composable
fun NavigationFragmentComposePlace(
    id: String,
    modifier: Modifier = Modifier,
    update: (NavigationFragmentContainerView) -> Unit = NoOpUpdate
) {
    AndroidView(
        {
            NavigationFragmentContainerView(it).apply {
                navigationTag = id
            }
        },
        modifier,
        update
    )
}
