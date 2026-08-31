import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import korlibs.time.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationChainRestartTests : NavigationTest() {
    private class TestConfig : NavigationNodeDefaultConfig {
        override val id: String = "node"
    }

    private class CountingNode(
        override val chain: NavigationChain<TestConfig>,
        config: TestConfig,
    ) : NavigationNode<TestConfig, TestConfig>(NavigationNodeId(config.id)) {
        override val configState: StateFlow<TestConfig> = MutableStateFlow(config).asStateFlow()

        private val _startCount = MutableStateFlow(0)
        val startCount = _startCount.asStateFlow()

        override fun start(scope: CoroutineScope): Job {
            _startCount.update { it + 1 }
            return super.start(scope)
        }
    }

    @Test
    fun restartingChainStartsExistingNodesAgain() = runTest(EmptyCoroutineContext + Job()) {
        val chain = NavigationChain<TestConfig>(
            null,
            NavigationNodeFactory { chain, config ->
                CountingNode(chain, config)
            }
        )
        val scopeJob = Job(coroutineContext.job)
        val chainScope = this + Dispatchers.Default + scopeJob
        val waitingDispatcher = Dispatchers.Default.limitedParallelism(1)
        var chainJob: Job? = null

        try {
            chainJob = chain.start(chainScope)
            val node = chain.push(TestConfig()) as CountingNode
            withContext(waitingDispatcher) {
                withTimeout(1_000.milliseconds) {
                    chain.stackFlow.first { stack ->
                        stack.any { it === node }
                    }
                }
            }

            chainJob.cancelAndJoin()
            val startCountBeforeRestart = node.startCount.value
            chainJob = chain.start(chainScope)
            withContext(waitingDispatcher) {
                withTimeoutOrNull(1_000.milliseconds) {
                    node.startCount.first { it > startCountBeforeRestart }
                }
            }

            assertEquals(
                startCountBeforeRestart + 1,
                node.startCount.value,
                "An existing node must be started again when its chain is restarted"
            )
        } finally {
            chainJob?.cancelAndJoin()
            scopeJob.cancelAndJoin()
        }
    }
}
