package dev.inmo.navigation.compose

import androidx.compose.runtime.Composable
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

@Composable
fun <Base> initNavigation(
    defaultStartChain: ConfigHolder.Chain<Base>,
    configsRepo: NavigationConfigsRepo<Base>,
    nodesFactory: NavigationNodeFactory<Base>,
    scope: CoroutineScope = rememberCoroutineScope(),
    dropRedundantChainsOnRestore: Boolean = false,
    rootChain: NavigationChain<Base> = NavigationChain(null, nodesFactory)
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
    logger.d { "Hierarchy saving enabled" }

    val existsChain = configsRepo.get()

    logger.d {
        if (existsChain == null) {
            "Can't find exists chain. Using default one: $defaultStartChain"
        } else {
            "Took exists stored chain $existsChain"
        }
    }

    val rootChain = remember {
        restoreHierarchy<Base>(
            existsChain ?: defaultStartChain,
            factory = resultNodesFactory,
            rootChain = rootChain,
            dropRedundantChainsOnRestore = dropRedundantChainsOnRestore
        )
    }
    rootChain ?.start(subscope)
    rootChain ?.StartInCompose({ savingJob.cancel() })
}
