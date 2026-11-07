package dev.inmo.navigation.core.fragments

import dev.inmo.kslog.common.d

internal interface CallbacksCollection {
    fun onNavigationCreate()
    fun onNavigationStart()
    fun onNavigationResume()
    fun onNavigationPause()
    fun onNavigationStop()
    fun onNavigationDestroy()

    suspend fun onBeforeNavigationCreate()
    suspend fun onBeforeNavigationStart()
    suspend fun onBeforeNavigationResume()
    suspend fun onBeforeNavigationPause()
    suspend fun onBeforeNavigationStop()
    suspend fun onBeforeNavigationDestroy()

    class Callbacks(
        val onNavigationCreateCallback: () -> Unit,
        val onNavigationStartCallback: () -> Unit,
        val onNavigationResumeCallback: () -> Unit,
        val onNavigationPauseCallback: () -> Unit,
        val onNavigationStopCallback: () -> Unit,
        val onNavigationDestroyCallback: () -> Unit,

        val onBeforeNavigationCreateCallback: suspend () -> Unit,
        val onBeforeNavigationStartCallback: suspend () -> Unit,
        val onBeforeNavigationResumeCallback: suspend () -> Unit,
        val onBeforeNavigationPauseCallback: suspend () -> Unit,
        val onBeforeNavigationStopCallback: suspend () -> Unit,
        val onBeforeNavigationDestroyCallback: suspend () -> Unit,
    ) : CallbacksCollection {
        override fun onNavigationCreate() {
            onNavigationCreateCallback()
        }

        override fun onNavigationStart() {
            onNavigationStartCallback()
        }

        override fun onNavigationResume() {
            onNavigationResumeCallback()
        }

        override fun onNavigationPause() {
            onNavigationPauseCallback()
        }

        override fun onNavigationStop() {
            onNavigationStopCallback()
        }

        override fun onNavigationDestroy() {
            onNavigationDestroyCallback()
        }

        override suspend fun onBeforeNavigationCreate() {
            onBeforeNavigationCreateCallback()
        }

        override suspend fun onBeforeNavigationStart() {
            onBeforeNavigationStartCallback()
        }

        override suspend fun onBeforeNavigationResume() {
            onBeforeNavigationResumeCallback()
        }

        override suspend fun onBeforeNavigationPause() {
            onBeforeNavigationPauseCallback()
        }

        override suspend fun onBeforeNavigationStop() {
            onBeforeNavigationStopCallback()
        }

        override suspend fun onBeforeNavigationDestroy() {
            onBeforeNavigationDestroyCallback()
        }
    }
}