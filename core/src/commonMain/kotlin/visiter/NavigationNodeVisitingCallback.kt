package dev.inmo.navigation.core.visiter

import dev.inmo.navigation.core.NavigationNode

typealias NavigationNodeVisitingCallback<Base> = (NavigationNode<out Base, Base>) -> Unit
