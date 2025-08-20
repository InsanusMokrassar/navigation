package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeId
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext

/**
 * Provides [onDraw] open function which will be called by the navigation system to draw content in the place it added
 */
abstract class ComposeNode<Config : Base, Base>(
    config: Config,
    override val chain: NavigationChain<Base>,
    id: NavigationNodeId = NavigationNodeId()
) : NavigationNode<Config, Base>(id) {
    protected val _configState = MutableStateFlow(config)
    override val configState: StateFlow<Config> = _configState.asStateFlow()
    override var config: Config
        get() = _configState.value
        set(value) { _configState.value = value }
    internal val drawerState: MutableRedeliverStateFlow<(@Composable () -> Unit)?> = MutableRedeliverStateFlow(null)

    override fun onResume() {
        super.onResume()
        drawerState.value = @Composable { onDraw() }
    }

    override fun onPause() {
        super.onPause()
        drawerState.value = null
    }

    private val _beforePauseWaitJobState = MutableRedeliverStateFlow<CompletableJob?>(null)

    /**
     * Will contain [CompletableJob] if [useBeforePauseWait] returns true before pausing will be done.
     *
     * This state will be useful to animate or do some before-pausing job. After job or animation is done,
     * call [CompletableJob.complete] on [beforePauseWaitJobState] value
     */
    val beforePauseWaitJobState = _beforePauseWaitJobState.asStateFlow()

    /**
     * In case you wish to do some job before pause, you may return true from this function and listen for
     * [beforePauseWaitJobState] states. If [beforePauseWaitJobState] is not null - you are in pre-pause state and may
     * do some job. After job is done, you MUST call [CompletableJob.complete] on [beforePauseWaitJobState] value
     */
    protected open suspend fun useBeforePauseWait(): Boolean = false

    override suspend fun onBeforePause() {
        super.onBeforePause()
        if (useBeforePauseWait()) {
            val completingJob = Job(coroutineContext.job)
            _beforePauseWaitJobState.value = completingJob
            runCatching {
                completingJob.join()
            }
        }
    }

    override suspend fun onBeforeStart() {
        super.onBeforeStart()
        _beforePauseWaitJobState.value = null
    }

    @Composable
    protected open fun onDraw() {}

    /**
     * Provides place for [NavigationChain] which placed in [subchainsFlow]
     */
    @Composable
    protected open fun SubchainsHost(filter: (NavigationChain<Base>) -> Boolean) {
        SubchainsHandling(filter)
    }

    @Composable
    protected fun SubchainsHost() = SubchainsHost { true }
}