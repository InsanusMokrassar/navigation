package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.repo.enableSavingHierarchy
import dev.inmo.navigation.core.repo.restoreHierarchy
import kotlinx.coroutines.CoroutineScope

/**
 * Creates root of navigation in current place
 *
 * @param defaultStartChain Config of default tree for navigation in case [configsRepo] contains no any information
 * about last used navigation
 * @param configsRepo Contains information about last saved navigation tree
 * @param nodesFactory Provides opportunity to create [dev.inmo.navigation.core.NavigationNode] from their configs
 * @param scope Will be used to create [LinkedSupervisorScope] which will be the root [CoroutineScope] for all navigation
 * operations
 * @param dropRedundantChainsOnRestore Drops chains with empty content
 * @param rootChain Default root chain where navigation will be rooted
 */
@Composable
private fun <Base> initNavigation(
    defaultStartChain: ConfigHolder.Chain<Base>?,
    configsRepo: NavigationConfigsRepo<Base>,
    nodesFactory: NavigationNodeFactory<Base>,
    scope: CoroutineScope = rememberCoroutineScope(),
    dropRedundantChainsOnRestore: Boolean = false,
    rootChain: NavigationChain<Base> = NavigationChain(null, nodesFactory),
    defaultInitBlock: @Composable () -> Unit = {}
) {
    val logger = TagLogger("NavigationJob")
    val subscope = scope.LinkedSupervisorScope()

    logger.d { "Start enable saving of hierarchy" }
    val savingJob = remember {
        configsRepo.enableSavingHierarchy(
            rootChain,
            subscope,
        )
    }
    val resultNodesFactory = remember {
        NavigationNodeFactory<Base> { navigationChain, config ->
            nodesFactory.createNode(navigationChain, config) ?.also {
                it.chain.enableSavingHierarchy(
                    configsRepo,
                    subscope
                )
            }
        }
    }
    doWithNodesFactoryInLocalProvider(resultNodesFactory) {
        logger.d { "Hierarchy saving enabled" }

        val existsChain = configsRepo.get()

        logger.d {
            if (existsChain == null) {
                "Can't find exists chain. Using default one: $defaultStartChain"
            } else {
                "Took exists stored chain $existsChain"
            }
        }

        val restoredRootChain = remember {
            restoreHierarchy<Base>(
                existsChain ?: defaultStartChain ?: ConfigHolder.Chain<Base>(null),
                factory = resultNodesFactory,
                rootChain = rootChain,
                dropRedundantChainsOnRestore = dropRedundantChainsOnRestore
            )
        }
        restoredRootChain ?.let {
            if (existsChain == null && defaultStartChain == null) {
                doWithChainInLocalProvider(it) {
                    defaultInitBlock()
                }
            } else {
                it.DrawStackNodes()
            }

            it.start(subscope)
        }
    }
}

/**
 * Creates root of navigation in current place
 *
 * @param defaultStartChain Config of default tree for navigation in case [configsRepo] contains no any information
 * about last used navigation
 * @param configsRepo Contains information about last saved navigation tree
 * @param nodesFactory Provides opportunity to create [dev.inmo.navigation.core.NavigationNode] from their configs
 * @param scope Will be used to create [LinkedSupervisorScope] which will be the root [CoroutineScope] for all navigation
 * operations
 * @param dropRedundantChainsOnRestore Drops chains with empty content
 * @param rootChain Default root chain where navigation will be rooted
 */
@Composable
fun <Base> initNavigation(
    defaultStartChain: ConfigHolder.Chain<Base>,
    configsRepo: NavigationConfigsRepo<Base>,
    nodesFactory: NavigationNodeFactory<Base>,
    scope: CoroutineScope = rememberCoroutineScope(),
    dropRedundantChainsOnRestore: Boolean = false,
    rootChain: NavigationChain<Base> = NavigationChain(null, nodesFactory),
) = initNavigation(
    defaultStartChain = defaultStartChain,
    configsRepo = configsRepo,
    nodesFactory = nodesFactory,
    scope = scope,
    dropRedundantChainsOnRestore = dropRedundantChainsOnRestore,
    rootChain = rootChain,
    defaultInitBlock = {}
)


/**
 * Creates root of navigation in current place
 *
 * @param defaultStartChain Config of default tree for navigation in case [configsRepo] contains no any information
 * about last used navigation
 * @param configsRepo Contains information about last saved navigation tree
 * @param nodesFactory Provides opportunity to create [dev.inmo.navigation.core.NavigationNode] from their configs
 * @param scope Will be used to create [LinkedSupervisorScope] which will be the root [CoroutineScope] for all navigation
 * operations
 * @param dropRedundantChainsOnRestore Drops chains with empty content
 * @param rootChain Default root chain where navigation will be rooted
 */
@Composable
fun <Base> initNavigation(
    rootNodeConfig: Base,
    configsRepo: NavigationConfigsRepo<Base>,
    nodesFactory: NavigationNodeFactory<Base>,
    scope: CoroutineScope = rememberCoroutineScope(),
    dropRedundantChainsOnRestore: Boolean = false,
    rootChain: NavigationChain<Base> = NavigationChain(null, nodesFactory),
    defaultInitBlock: @Composable () -> Unit
) = initNavigation(
    defaultStartChain = null,
    configsRepo = configsRepo,
    nodesFactory = nodesFactory,
    scope = scope,
    dropRedundantChainsOnRestore = dropRedundantChainsOnRestore,
    rootChain = rootChain
) {
    InjectNavigationNode(
        rootNodeConfig,
        additionalCodeInNodeContext = defaultInitBlock
    )
}
