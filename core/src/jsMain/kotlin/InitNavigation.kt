package dev.inmo.navigation.core

import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.d
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.navigation.core.configs.NavigationNodeDefaultConfig
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.repo.enableSavingHierarchy
import dev.inmo.navigation.core.repo.restoreHierarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Base, reified Base : NavigationNodeDefaultConfig> initNavigation(
    startChain: ConfigHolder.Chain<Base>,
    configsRepo: NavigationConfigsRepo<Base> = CookiesNavigationConfigsRepo(
        Json { ignoreUnknownKeys = true },
        ConfigHolder.serializer(Base::class.serializer())
    ),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    savingDebounce: Long = 0L,
    rootChain: NavigationChain<Base>,
    nodesFactory: NavigationNodeFactory<Base>
): Job {
    val logger = TagLogger("NavigationJob")
    val subscope = scope.LinkedSupervisorScope()

    return subscope.launch {
        logger.d { "Start enable saving of hierarchy" }
        configsRepo.enableSavingHierarchy(
            rootChain,
            this,
        )
        val resultNodesFactory = NavigationNodeFactory<Base> { navigationChain, config ->
            nodesFactory.createNode(navigationChain, config) ?.also {
                it.chain.enableSavingHierarchy(
                    configsRepo,
                    subscope,
                    debounce = savingDebounce
                )
            }
        }
        logger.d { "Hierarchy saving enabled" }

        val existsChain = configsRepo.get()

        logger.d {
            if (existsChain == null) {
                "Can't find exists chain. Using default one: $startChain"
            } else {
                "Took exists stored chain $existsChain"
            }
        }

        restoreHierarchy<Base>(
            existsChain ?: startChain,
            factory = resultNodesFactory,
            rootChain = rootChain
        ) ?.start(subscope)
    }
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Base, reified Base : NavigationNodeDefaultConfig> initNavigation(
    startChain: ConfigHolder.Chain<Base>,
    configsRepo: NavigationConfigsRepo<Base> = CookiesNavigationConfigsRepo(
        Json { ignoreUnknownKeys = true },
        ConfigHolder.serializer(Base::class.serializer())
    ),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    savingDebounce: Long = 0L,
    nodesFactory: NavigationNodeFactory<Base>
): Job = initNavigation(
    startChain,
    configsRepo,
    scope,
    savingDebounce,
    NavigationChain<Base>(null, nodesFactory),
    nodesFactory
)
