package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.NO_ID
import android.view.ViewGroup.OnHierarchyChangeListener
import androidx.annotation.IdRes
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.common.findViewsByTag
import dev.inmo.micro_utils.common.findViewsByTagInActivity
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.utils.FlowOnHierarchyChangeListener
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

class AndroidFragmentNode<Config : Any>(
    override val chain: NavigationChain<Config>,
    config: Config,
    private val viewTag: String,
    private val fragmentKClass: KClass<out NodeFragment<Config>>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener,
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

    private fun placeFragment(view: View) {
        fragment ?.let {
            view.id = view.id.takeIf { it != NO_ID } ?: View.generateViewId()
            fragmentManager.beginTransaction().apply {
                runCatching {
                    replace(view.id, it)
                }.onSuccess {
                    commit()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fragment ?.let {
            findViewsByTag(rootView, viewTag).firstOrNull() ?.also { view ->
                placeFragment(view)
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

    override fun onDestroy() {
        super.onDestroy()
        fragment = null
    }

    override fun start(scope: CoroutineScope): Job {
        val subsubscope = scope.LinkedSupervisorScope()
        return super.start(subsubscope).let {

            flowOnHierarchyChangeListener.onChildViewAdded.subscribeSafelyWithoutExceptions(subsubscope) { (_, child) ->
                fragment ?.let {
                    if (viewTag == child.tag && state == NavigationNodeState.RESUMED && !it.isAdded) {
                        placeFragment(child)
                    }
                }
            }

            subsubscope.coroutineContext.job
        }
    }
}
