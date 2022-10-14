package dev.inmo.navigation.core.fragments

import androidx.fragment.app.Fragment
import dev.inmo.navigation.core.AndroidNodeConfig

abstract class NodeFragment<Config : AndroidNodeConfig> : Fragment() {
    protected lateinit var node: AndroidFragmentNode<Config>
        private set
}
