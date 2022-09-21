package dev.inmo.navigation.mvvm.sample.android

import android.app.Application
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class App : Application() {
    val rootChain by lazy {
        NavigationChain<AndroidNodeConfig>(null, CoroutineScope(Dispatchers.Main))
    }
    val rootNode by lazy {
        NavigationNode.Empty<AndroidNodeConfig>(rootChain)
    }
}
