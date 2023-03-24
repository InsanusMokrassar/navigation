package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.NavigationChain

typealias NavigationChainVisitingCallback<Base> = (NavigationChain<Base>) -> Unit
