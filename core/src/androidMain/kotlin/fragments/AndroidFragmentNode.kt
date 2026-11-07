package dev.inmo.navigation.core.fragments

import android.view.View
import android.view.ViewGroup.NO_ID
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.fragments.transactions.FragmentTransactionConfigurator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

class AndroidFragmentNode<Config : Base, Base : NavigationNodeDefaultConfig>(
    override val chain: NavigationChain<Base>,
    config: Config,
    private val fragmentKClass: KClass<*>,
    private val fragmentManager: FragmentManager,
    private val rootView: View,
    private val flowOnHierarchyChangeListener: FlowOnHierarchyChangeListener,
    private val manualHierarchyCheckerDelayMillis: Long? = 1000L,
    override val id: NavigationNodeId = NavigationNodeId(),
    private val fragmentTransactionConfigurator: FragmentTransactionConfigurator<Base>? = null
) : NavigationNode<Config, Base>() {
    override val log: KSLog by lazy {
        TagLogger("${this::class.simpleName}/${fragmentKClass.simpleName}")
    }
    private val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
    private val viewTag
        get() = config.id

    private val _beforePauseWaitJobState = MutableRedeliverStateFlow<CompletableJob?>(null)
    private val descendantViews = MutableStateFlow<Map<NodeFragment<*, *>, Lifecycle>>(emptyMap())

    internal fun onDescendantViewCreated(fragment: NodeFragment<*, *>, lifecycle: Lifecycle) {
        descendantViews.value += fragment to lifecycle
    }

    internal fun onDescendantViewDestroyed(fragment: NodeFragment<*, *>) {
        descendantViews.value -= fragment
    }

    /**
     * In case you wish to do some job before pause, you may return true from this function and listen for
     * [beforePauseWaitJobState] states. If [beforePauseWaitJobState] is not null - you are in pre-pause state and may
     * do some job. After job is done, you MUST call [CompletableJob.complete] on [beforePauseWaitJobState] value
     */
    var useBeforePauseWait: Boolean = false
    /**
     * Will contain [CompletableJob] if [useBeforePauseWait] returns true before pausing will be done.
     *
     * This state will be useful to animate or do some before-pausing job. After job or animation is done,
     * call [CompletableJob.complete] on [beforePauseWaitJobState] value
     */
    val beforePauseWaitJobState = _beforePauseWaitJobState.asStateFlow()

    private var fragment: NodeFragment<Config, Base>? = ((fragmentKClass.objectInstance ?: fragmentKClass.constructors.first {
        it.parameters.isEmpty()
    }.call()) as NodeFragment<Config, Base>).also {
        it.setNode(this, _configState)
    }

    override suspend fun onBeforePause() {
        super.onBeforePause()
        if (useBeforePauseWait) {
            val completingJob = Job(coroutineContext.job)
            _beforePauseWaitJobState.value = completingJob
            runCatching {
                completingJob.join()
            }
        }
        awaitDescendantEffects()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitDescendantEffects() = withContext(Dispatchers.Main.immediate) {
        val hostLifecycle = fragment?.activity?.lifecycle ?: return@withContext
        // A prepared framework transition receives no end callback if its container
        // detaches before pre-draw. Keep the ancestor resumed until its descendants
        // have finished entering/exiting. Backgrounding keeps this wait suspended;
        // destroying the host cancels it along with the navigation job.
        combine(
            hostLifecycle.currentStateFlow,
            descendantViews.flatMapLatest { views ->
                if (views.isEmpty()) {
                    flowOf(Unit)
                } else {
                    combine(views.values.map { it.currentStateFlow }) { Unit }
                }
            }
        ) { _, _ -> Unit }.first {
            val hostState = hostLifecycle.currentState
            if (hostState == Lifecycle.State.DESTROYED) {
                true
            } else if (hostState != Lifecycle.State.RESUMED) {
                false
            } else {
                // Leaving an ancestor also leaves every postponed child container.
                // Release postponement just as a new transaction in that container
                // would, then wait for the actual effect completion.
                descendantViews.value.keys.forEach { it.startPostponedEnterTransition() }
                // Read the current registry on Main, not a cached combine result:
                // another child effect may have begun while a flow update was queued.
                descendantViews.value.values.all {
                    it.currentState == Lifecycle.State.RESUMED || it.currentState == Lifecycle.State.DESTROYED
                }
            }
        }
    }

    private fun placeFragment(view: View) {
        fragment ?.let {
            view.id = view.id.takeIf { it != NO_ID } ?: View.generateViewId()
            fragmentManager.beginTransaction().apply {
                // Batch remove/replace operations so their framework transitions complete together.
                setReorderingAllowed(true)
                runCatching {
                    fragmentTransactionConfigurator ?.apply {
                        onPlace(this@AndroidFragmentNode)
                    }
                    if (it.hasInactiveAncestor) {
                        setCustomAnimations(0, 0, 0, 0)
                        setTransition(FragmentTransaction.TRANSIT_NONE)
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
                setReorderingAllowed(true)
                runCatching {
                    fragmentTransactionConfigurator ?.apply {
                        onRemove(this@AndroidFragmentNode)
                    }
                    if (it.hasInactiveAncestor) {
                        // Suppressing a framework transition must not enable its
                        // legacy animation fallback in the departing subtree.
                        setCustomAnimations(0, 0, 0, 0)
                        setTransition(FragmentTransaction.TRANSIT_NONE)
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

            stateFlow.filter { it == NavigationNodeState.RESUMED }.subscribeLoggingDropExceptions(subscope) {
                val subsubscope = subscope.LinkedSupervisorScope()

                flowOnHierarchyChangeListener.onChildViewAdded.filter {
                    log.d { "Added views: ${it}, subview navigation tag: ${it.second.navigationTag}" }
                    it.second.navigationTag == viewTag && chain.stack.contains(this@AndroidFragmentNode)
                }.subscribeLoggingDropExceptions(subsubscope) {
                    placeFragment()
                }

                onDestroyFlow.subscribeLoggingDropExceptions(subsubscope) {
                    subsubscope.cancel()
                }

                merge(
                    stateFlow,
                    chain.stackFlow,
                    flow {
                        while (true) {
                            delay(manualHierarchyCheckerDelayMillis ?: return@flow)
                            emit(Unit)
                        }
                    }
                )
                    .takeWhile {
                        state == NavigationNodeState.RESUMED && chain.stack.contains(this@AndroidFragmentNode)
                    }
                    .subscribeLoggingDropExceptions(subsubscope) {
                        if (fragment ?.isAdded != true) {
                            placeFragment()
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
