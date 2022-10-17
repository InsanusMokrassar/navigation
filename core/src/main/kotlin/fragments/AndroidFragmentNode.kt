package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup.NO_ID
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.common.findViewsByTag
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.*
import kotlinx.coroutines.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class AndroidFragmentNode<Config : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Config>,
    override var config: Config,
    private val fragmentKClass: KClass<out NodeFragment<Config>>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener,
    override val id: NavigationNodeId = NavigationNodeId()
) : NavigationNode<Config>() {
    private val viewTag
        get() = config.id
    private var fragment: NodeFragment<Config>? = null

    override fun onCreate() {
        super.onCreate()
        fragment = fragmentKClass.objectInstance ?: fragmentKClass.constructors.first {
            it.parameters.isEmpty()
        }.call()
    }

    override fun onStart() {
        super.onStart()
        val bundle = bundleOf(
            *config::class.members.mapNotNull {
                if (it is KProperty<*>) {
                    it.name to it.getter.call(config)
                } else {
                    null
                }
            }.toTypedArray()
        )
        fragment ?.arguments = bundle
        fragment ?.setNode(this)
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
            findViewsByTag(rootView, navigationTagKey, viewTag).firstOrNull() ?.also { view ->
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
                    if (viewTag == child.navigationTag && state == NavigationNodeState.RESUMED && !it.isAdded) {
                        placeFragment(child)
                    }
                }
            }

            subsubscope.coroutineContext.job
        }
    }
}
