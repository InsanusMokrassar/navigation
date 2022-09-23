package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
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
                }.onSuccess {
                    commit()
                }

                fun placeFragment(view: View) {
                    view.id = view.id ?: View.generateViewId()
                    replace(view.id, it)
                }

                findViewsByTag(rootView, viewTag).firstOrNull() ?.also { view ->
                    view.id = view.id ?: View.generateViewId()
                    replace(view.id, it)
                } ?: (rootView as? ViewGroup) ?.let {
                    lateinit var listener: OnHierarchyChangeListener
                    listener = object : OnHierarchyChangeListener {
                        override fun onChildViewAdded(parent: View?, child: View?) {
                            if (child ?.tag == viewTag) {
                                placeFragment(child)
                            } else {
                                (child as? ViewGroup) ?.setOnHierarchyChangeListener(listener
                                )
                            }
                        }

                        override fun onChildViewRemoved(parent: View?, child: View?) {}
                    }
                    it.setOnHierarchyChangeListener(listener)
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
