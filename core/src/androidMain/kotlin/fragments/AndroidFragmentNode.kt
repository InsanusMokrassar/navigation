package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup.NO_ID
import androidx.fragment.app.FragmentManager
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

class AndroidFragmentNode<Config : Base, Base : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Base>,
    config: Config,
    private val fragmentKClass: KClass<*>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener,
    private val manualHierarchyCheckerDelayMillis: Long? = 100L,
    override val id: NavigationNodeId = NavigationNodeId(),
    private val fragmentTransactionConfigurator: FragmentTransactionConfigurator<Base>? = null
) : NavigationNode<Config, Base>() {
    override val log: KSLog by lazy {
        TagLogger("${this::class.simpleName}/${fragmentKClass.simpleName}")
    }
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
                    fragmentTransactionConfigurator ?.apply {
                        onPlace(this@AndroidFragmentNode)
                    }
                    replace(view.id, it)
                }.onSuccess {
                    commit()
                }
            }
        }
    }

    private fun placeFragment(): Boolean {
        return rootView.findViewsWithNavigationTag(viewTag).firstOrNull() ?.also(::placeFragment) != null
    }

    override fun onPause() {
        super.onPause()

        fragment ?.let {
            fragmentManager.beginTransaction().apply {
                runCatching {
                    fragmentTransactionConfigurator ?.apply {
                        onRemove(this@AndroidFragmentNode)
                    }
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
        val subscope = scope.LinkedSupervisorScope()
        return super.start(subscope).let {

            (flowOf(state) + statesFlow).filter { it == NavigationNodeState.RESUMED }.subscribeSafelyWithoutExceptions(subscope) {
                val subsubscope = subscope.LinkedSupervisorScope()

//                placeFragment()

                flowOnHierarchyChangeListener.onChildViewAdded.filter {
                    log.d { "Added views: ${it}, subview navigation tag: ${it.second.navigationTag}" }
                    it.second.navigationTag == viewTag
                }.subscribeSafelyWithoutExceptions(subsubscope) {
                    placeFragment()
                }

                onDestroyFlow.subscribeSafelyWithoutExceptions(subsubscope) {
                    subsubscope.cancel()
                }

                subsubscope.launchSafelyWithoutExceptions {
                    while (state == NavigationNodeState.RESUMED) {
                        if (fragment ?.isAdded != true) {
                            placeFragment()
                        }

                        delay(manualHierarchyCheckerDelayMillis ?: break)
                    }
                }
            }

            subscope.coroutineContext.job
        }
    }

    override fun toString(): String {
        return "${super.toString()}/${fragmentKClass.simpleName}"
    }
}
