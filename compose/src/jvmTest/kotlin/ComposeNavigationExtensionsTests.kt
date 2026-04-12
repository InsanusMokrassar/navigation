import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.navigation.compose.ComposeInjectedChainsAndNodes
import dev.inmo.navigation.compose.ComposeNode
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.compose.doWithComposeInjectedChainsAndNodesInLocalProvider
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ComposeNavigationExtensionsTests {
    private data class TestConfig(override val id: String) : NavigationNodeDefaultConfig
    private data class EmptyConfig(override val id: String) : NavigationNodeDefaultConfig
    private class TestComposeView(
        val drawReport: @Composable (ComposeNode<out NavigationNodeDefaultConfig, NavigationNodeDefaultConfig>) -> Unit,
        config: TestConfig,
        chain: NavigationChain<NavigationNodeDefaultConfig>,
        id: NavigationNodeId = NavigationNodeId()
    ) : ComposeNode<TestConfig, NavigationNodeDefaultConfig>(
        config,
        chain,
        id
    ) {
        @Composable
        override fun onDraw() {
            super.onDraw()
            drawReport(this)
            SubchainsHost()
        }
    }
    private class TestNodeFactory(private val onDraw: @Composable (ComposeNode<out NavigationNodeDefaultConfig, NavigationNodeDefaultConfig>) -> Unit) : NavigationNodeFactory<NavigationNodeDefaultConfig> {
        override fun createNode(
            navigationChain: NavigationChain<NavigationNodeDefaultConfig>,
            config: NavigationNodeDefaultConfig
        ): NavigationNode<out NavigationNodeDefaultConfig, NavigationNodeDefaultConfig>? {
            return when (config) {
                is TestConfig -> TestComposeView(onDraw, config, navigationChain)
                is EmptyConfig -> NavigationNode.Empty(navigationChain, config)
                else -> null
            }
        }

    }
    @Test
    fun injectNavigationNodeTest() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val drawingsCalculatorMutex = Mutex()
        var root: NavigationChain<NavigationNodeDefaultConfig>? = null
        setContent {
            initNavigation(
                EmptyConfig("root"),
                NavigationConfigsRepo.InMemory(),
                TestNodeFactory {
                    val state = 0
                    LaunchedEffect(state) {
                        root = it.chain.rootChain()
                        drawingsCalculatorMutex.withLock {
                            drawingsCalculator.add(it.config.id)
                        }
                    }
                }
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    InjectNavigationNode(
                        TestConfig("test")
                    )
                }
            }
        }

        waitForIdle()

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test")
        )
        assertNotNull(root)
        assertEquals(
            1,
            root.stackFlow.value.size
        )
        assertTrue(root.stackFlow.value.first().config is EmptyConfig)
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.size
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.size
        )
        assertTrue(
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config is TestConfig
        )
        assertEquals(
            "test",
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config.id
        )
    }
    @Test
    fun multiInjectNavigationNodeTest() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val drawingsCalculatorMutex = Mutex()
        var root: NavigationChain<NavigationNodeDefaultConfig>? = null
        setContent {
            initNavigation(
                EmptyConfig("root"),
                NavigationConfigsRepo.InMemory(),
                TestNodeFactory {
                    val state = 0
                    LaunchedEffect(state) {
                        root = it.chain.rootChain()
                        drawingsCalculatorMutex.withLock {
                            drawingsCalculator.add(it.config.id)
                        }
                    }
                }
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    InjectNavigationNode(
                        TestConfig("test0")
                    ) {
                        InjectNavigationChain<NavigationNodeDefaultConfig> {
                            InjectNavigationNode(
                                TestConfig("test1")
                            ) {
                                InjectNavigationChain<NavigationNodeDefaultConfig> {
                                    InjectNavigationNode(
                                        TestConfig("test2")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        waitForIdle()

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test0", "test1", "test2")
        )
        assertNotNull(root)
        assertEquals(
            1,
            root.stackFlow.value.size
        )
        assertTrue(root.stackFlow.value.first().config is EmptyConfig)
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.size
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.size
        )
        assertTrue(
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config is TestConfig
        )
        assertEquals(
            "test0",
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config.id
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.size
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.size
        )
        assertTrue(
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().config is TestConfig
        )
        assertEquals(
            "test1",
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().config.id
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.size
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.size
        )
        assertTrue(
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().config is TestConfig
        )
        assertEquals(
            "test2",
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().subchains.first().stackFlow.value.first().config.id
        )
    }
    @Test
    fun doubleInjectNavigationNodeTest() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val drawingsCalculatorMutex = Mutex()
        val startAssertions = Job()
        setContent {
            initNavigation(
                EmptyConfig("root"),
                NavigationConfigsRepo.InMemory(),
                TestNodeFactory {
                    val state = 0
                    LaunchedEffect(state) {
                        drawingsCalculatorMutex.withLock {
                            drawingsCalculator.add(it.config.id)
                        }
                    }
                }
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    InjectNavigationNode(
                        TestConfig("test1")
                    )
                    val addTest2 = remember { mutableStateOf(false) }
                    if (addTest2.value) {
                        InjectNavigationNode(
                            TestConfig("test2")
                        ) {
                            LaunchedEffect(Unit) {
                                startAssertions.complete()
                            }
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            addTest2.value = true
                        }
                    }
                }
            }
        }

        startAssertions.join()
        waitForIdle()

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test1", "test2")
        )
    }
    @Test
    fun pushConfigTest() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val drawingsCalculatorMutex = Mutex()
        setContent {
            initNavigation(
                EmptyConfig("root"),
                NavigationConfigsRepo.InMemory(),
                TestNodeFactory {
                    val state = 0
                    LaunchedEffect(state) {
                        drawingsCalculatorMutex.withLock {
                            drawingsCalculator.add(it.config.id)
                        }
                    }
                }
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    LaunchedEffect(Unit) {
                        push(TestConfig("test"))
                    }
                }
            }
        }

        waitForIdle()

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test")
        )
    }
    @Test
    fun doublePushConfigTest() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val waitJob = Job()
        val navigationConfigsRepo = NavigationConfigsRepo.InMemory<NavigationNodeDefaultConfig>()
        val drawingsFlow = MutableRedeliverStateFlow<Int>(0)
        setContent {
            initNavigation(
                EmptyConfig("root"),
                navigationConfigsRepo,
                TestNodeFactory {
                    remember(Unit) {
                        drawingsFlow.value++
                        drawingsCalculator.add(it.config.id)
                    }
                }
            ) {
                InjectNavigationChain<NavigationNodeDefaultConfig> {
                    LaunchedEffect(Unit) {
                        val node = push(TestConfig("test1"))
                        node ?.statesFlow ?.firstOrNull { it == NavigationNodeState.RESUMED }
                        drawingsFlow.firstOrNull { it == 1 }
                        val secondNode = push(TestConfig("test2"))
                        secondNode ?.statesFlow ?.firstOrNull { it == NavigationNodeState.RESUMED }
                        drawingsFlow.firstOrNull { it == 2 }
                        waitJob.complete()
                    }
                }
            }
        }

        waitJob.join()
        waitForIdle()
        println(navigationConfigsRepo.get())

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test1", "test2")
        )
    }

    @Test
    fun testInternalManagementOfDrawnChainsWorksProperly() = runComposeUiTest {
        val drawingsCalculator = mutableListOf<String>()
        val drawingsCalculatorMutex = Mutex()
        var root: NavigationChain<NavigationNodeDefaultConfig>? = null
        val dropNode = mutableStateOf(false)
        val dropChain = mutableStateOf(false)
        val composeInjectedChainsAndNodes = ComposeInjectedChainsAndNodes()
        setContent {
            doWithComposeInjectedChainsAndNodesInLocalProvider(
                composeInjectedChainsAndNodes
            ) {
                initNavigation(
                    EmptyConfig("root"),
                    NavigationConfigsRepo.InMemory(),
                    TestNodeFactory {
                        val state = 0
                        LaunchedEffect(state) {
                            root = it.chain.rootChain()
                            drawingsCalculatorMutex.withLock {
                                drawingsCalculator.add(it.config.id)
                            }
                        }
                    }
                ) {
                    if (dropChain.value == false) {
                        InjectNavigationChain<NavigationNodeDefaultConfig> {
                            if (dropNode.value == false) {
                                InjectNavigationNode(
                                    TestConfig("test")
                                )
                            }
                        }
                    }
                }
            }
        }

        waitForIdle()

        assertEquals(
            drawingsCalculator.toList(),
            listOf("test")
        )
        assertNotNull(root)
        assertEquals(
            1,
            root.stackFlow.value.size
        )
        assertTrue(root.stackFlow.value.first().config is EmptyConfig)
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.size
        )
        assertEquals(
            1,
            root.stackFlow.value.first().subchains.first().stackFlow.value.size
        )
        assertTrue(
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config is TestConfig
        )
        assertEquals(
            "test",
            root.stackFlow.value.first().subchains.first().stackFlow.value.first().config.id
        )

        assertTrue(composeInjectedChainsAndNodes.chains.single() === root.stackFlow.value.first().subchains.first())
        assertTrue(composeInjectedChainsAndNodes.nodes.size == 2)
        assertTrue(composeInjectedChainsAndNodes.nodes.last() === root.stackFlow.value.first().subchains.first().stackFlow.value.first())

        dropNode.value = true

        waitForIdle()

        assertTrue(composeInjectedChainsAndNodes.chains.single() === root.stackFlow.value.first().subchains.first())
        assertTrue(composeInjectedChainsAndNodes.nodes.size == 1)

        dropChain.value = true

        waitForIdle()

        assertTrue(composeInjectedChainsAndNodes.chains.isEmpty())
        assertTrue(composeInjectedChainsAndNodes.nodes.size == 1)
    }
}
