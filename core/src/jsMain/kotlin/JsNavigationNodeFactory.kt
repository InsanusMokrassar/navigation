package dev.inmo.navigation.core

typealias JsNavigationNodeFactory<Config, Base> = (Base) -> NavigationNode<Config, Base>
