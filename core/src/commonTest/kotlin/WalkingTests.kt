import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.common.either
import dev.inmo.navigation.core.ChainOrNodeEither
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeId
import dev.inmo.navigation.core.chainOrThrow
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.nodeOrThrow
import dev.inmo.navigation.core.onChain
import dev.inmo.navigation.core.onNode
import dev.inmo.navigation.core.visiter.walk
import kotlin.test.Test
import kotlin.test.assertEquals

class WalkingTests : NavigationTest() {
    private class TestConfig : NavigationNodeDefaultConfig {
        override val id: String = uuid4().toString()
    }

    /**
     * ```
     *         c1 - n1
     *       /
     * c0 - n0 - n3
     *       \
     *         c2 - n2
     * ```
     */
    @Test
    fun testingThatOrdinaryWalkingWorksProperly() {
        val c0 = NavigationChain<TestConfig>(
            null,
            NavigationNodeFactory { chain, config ->
                NavigationNode.Empty(chain, config, NavigationNodeId(config.id))
            }
        )
        val n0 = c0.push(TestConfig()) ?: error("Unable to create n0 for root chain for some reason")
        val (n1, c1) = n0.createSubChain(TestConfig()) ?: error("Unable to create subchain c1 with node n1 in n1 node for some reason")
        val n3 = c0.push(TestConfig()) ?: error("Unable to create node n2 for root chain for some reason")
        val (n2, c2) = n0.createSubChain(TestConfig()) ?: error("Unable to create subchain c2 with node n3 in n1 node for some reason")

        val orderOfVisiting = mutableListOf<ChainOrNodeEither<TestConfig>>(
            c0.either(),
            n0.either(),
            c1.either(),
            n1.either(),
            c2.either(),
            n2.either(),
            n3.either(),
        )

        c0.walk {
            it.onNode { visitingNode ->
                assertEquals(orderOfVisiting.removeFirst().nodeOrThrow, visitingNode)
            }.onChain { visitingChain ->
                assertEquals(orderOfVisiting.removeFirst().chainOrThrow, visitingChain)
            }
        }

        assertEquals(0, orderOfVisiting.size)
    }
}
