package dev.inmo.navigation.core

import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.Storage
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StorageNavigationRecoveryTests {
    @Serializable
    private sealed interface TestConfig : NavigationNodeDefaultConfig {
        @Serializable
        @SerialName("current")
        data class Current(override val id: String) : TestConfig
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ConfigHolder.serializer(TestConfig.serializer())

    private fun holder(id: String): ConfigHolder.Chain<TestConfig> = ConfigHolder.Chain(
        ConfigHolder.Node(TestConfig.Current(id), null, emptyList())
    )

    private fun memoryStorage(): Storage {
        val values = mutableMapOf<String, String>()
        // Storage is an external browser class. Supply its API without depending on a DOM in Node.js.
        val storage = js("({})")
        storage.getItem = { key: String -> values[key] }
        storage.setItem = { key: String, value: String -> values[key] = value }
        storage.removeItem = { key: String -> values.remove(key); Unit }
        storage.clear = { values.clear() }
        storage.key = { index: Int -> values.keys.elementAtOrNull(index) }
        return storage.unsafeCast<Storage>()
    }

    private fun assertNavigationStarts(savedValue: String?, expectedId: String) = runTest {
        val storage = memoryStorage()
        val key = "navigation-recovery-test"
        if (savedValue != null) storage.setItem(key, savedValue)
        val repo = StorageNavigationConfigsRepo(json, serializer, key, storage)
        val failures = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            StandardTestDispatcher(testScheduler) + SupervisorJob() +
                CoroutineExceptionHandler { _, error -> failures.add(error) }
        )
        val factory = NavigationNodeFactory<TestConfig> { chain, config ->
            NavigationNode.Empty(chain, config)
        }
        val root = NavigationChain(null, factory)

        try {
            initNavigation(
                startChain = holder("default"),
                configsRepo = repo,
                scope = scope,
                rootChain = root,
                nodesFactory = factory,
            )

            val resumedNode = withTimeoutOrNull(1_000) {
                val node = root.stackFlow.first { it.isNotEmpty() }.single()
                node.stateFlow.first { it == NavigationNodeState.RESUMED }
                node
            }
            assertNotNull(
                resumedNode,
                "Navigation must start with '$expectedId'; startup failures: ${failures.joinToString()}",
            )
            assertEquals(expectedId, resumedNode.config.id)
            assertTrue(failures.isEmpty(), "Startup must recover without an uncaught exception: $failures")
        } finally {
            scope.cancel()
            runCurrent()
            storage.clear()
        }
    }

    @Test
    fun missingSavedStateStartsDefaultNavigation() = assertNavigationStarts(null, "default")

    @Test
    fun validSavedStateRestoresSavedNavigation() = assertNavigationStarts(
        json.encodeToString(serializer, holder("saved")),
        "saved",
    )

    @Test
    fun malformedSavedStateFallsBackToDefaultNavigation() = assertNavigationStarts(
        "{broken-json",
        "default",
    )

    @Test
    fun removedConfigTypeFallsBackToDefaultNavigation() = assertNavigationStarts(
        """
            {
                "type": "Chain",
                "firstNodeConfig": {
                    "config": {"type": "retired", "id": "old-screen"},
                    "subnode": null,
                    "subchains": []
                }
            }
        """.trimIndent(),
        "default",
    )
}
