package dev.inmo.navigation.compose

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.*
import dev.inmo.navigation.core.extensions.onNodeAddedFlow
import dev.inmo.navigation.core.repo.ConfigHolder
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

abstract class NavigationComposeSingleActivity<Base : Any> : AppCompatActivity(), KoinComponent {
    protected abstract val baseClassName: KClass<Base>
    protected abstract fun createInitialConfigChain(): ConfigHolder.Chain<Base>

    @Composable
    protected open fun LoadingContent() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val showLoading = remember { mutableStateOf(false) }
            if (showLoading.value) {
                LoadingContent()
            }

            val scope = rememberCoroutineScope()

            val beforeIsDone = remember { mutableStateOf(false) }

            scope.launchSafelyWithoutExceptions {
                kotlin.runCatching { onBeforeStartNavigation() }
                beforeIsDone.value = true
            }

            if (beforeIsDone.value) {
                val rootChain = onStartNavigation(scope)

                rootChain.onNodeAddedFlow.subscribeSafelyWithoutExceptions(scope) {
                    showLoading.value = it.isEmpty()
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }

    protected open suspend fun onBeforeStartNavigation() {}
    @Composable
    protected open fun onStartNavigation(scope: CoroutineScope): NavigationChain<Base> {
        val rootChain = NavigationChain<Base>(null, nodeFactory = getKoin().nodeFactory())

        initNavigation(
            defaultStartChain = createInitialConfigChain(),
            configsRepo = AndroidSPConfigsRepo(
                getSharedPreferences("internal", MODE_PRIVATE),
                baseClassName,
                "navigation",
                getKoin().get()
            ),
            nodesFactory = getKoin().nodeFactory(),
            dropRedundantChainsOnRestore = true,
            scope = scope,
            rootChain = rootChain
        )
        return rootChain
    }
}
