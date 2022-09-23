package dev.inmo.navigation.core.fragments

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.common.findViewsByTag
import dev.inmo.micro_utils.common.findViewsByTagInActivity
import dev.inmo.navigation.core.*
import kotlin.reflect.KClass

class AndroidFragmentNode<Config : Any>(
    override val chain: NavigationChain<Config>,
    config: Config,
    private val viewTag: String,
    private val fragmentKClass: KClass<out NodeFragment<Config>>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
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
                    findViewsByTag(rootView, viewTag) ?.firstOrNull() ?.let { view ->
                        view.id = view.id ?: View.generateViewId()
                        replace(view.id, it)
                    }
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
