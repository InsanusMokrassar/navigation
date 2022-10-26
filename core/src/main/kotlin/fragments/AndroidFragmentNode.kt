package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup.NO_ID
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import dev.inmo.micro_utils.common.findViewsByTag
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class AndroidFragmentNode<Config : Base, Base : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Base>,
    config: Config,
    private val fragmentKClass: KClass<*>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener,
    override val id: NavigationNodeId = NavigationNodeId()
) : NavigationNode<Config, Base>() {
    private var fragment: NodeFragment<Config, Base>? = null
    private val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
    private val viewTag
        get() = config.id

    override fun onCreate() {
        super.onCreate()
        fragment = (fragmentKClass.objectInstance ?: fragmentKClass.constructors.first {
            it.parameters.isEmpty()
        }.call()) as NodeFragment<Config, Base>
        fragment ?.setNode(this, _configState)
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
