package dev.inmo.navigation.core.fragments

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import dev.inmo.navigation.core.*
import kotlin.reflect.KClass

class AndroidFragmentNode<Config : Any>(
    override val chain: NavigationChain<Config>,
    config: Config,
    @IdRes
    private val viewId: Int,
    private val fragmentKClass: KClass<out NodeFragment<Config>>,
    private val fragmentManager: FragmentManager,
    override val id: NavigationNodeId = NavigationNodeId()
) : NavigationNode<Config>() {
    override var config: Config = config
        private set
    private var fragment: NodeFragment<Config>? = null

    override fun onCreate() {
        super.onCreate()
        fragment = fragmentKClass.objectInstance ?: fragmentKClass.constructors.first {
            it.parameters.isEmpty()
        }.call()
    }

    override fun onStart() {
        super.onStart()
        fragment ?.configure(this) {
            config = it
        }
    }

    override fun onResume() {
        super.onResume()
        fragment ?.let {
            fragmentManager.beginTransaction().apply {
                runCatching {
                    replace(viewId, it)
                }.onSuccess {
                    commit()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        fragment ?.let {
            fragmentManager.beginTransaction().apply {
                runCatching {
                    remove(it)
                }.onSuccess {
                    commit()
                }
            }
        }
    }
}
