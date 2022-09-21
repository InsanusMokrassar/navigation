package dev.inmo.navigation.core.subject

import dev.inmo.kslog.common.e
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode

open class SubjectNavigationNode<Config, SubjectConfig : Config, Subject>(
    override val chain: NavigationChain<Config>,
    protected val subjectNavigationNodeConfigurator: SubjectNavigationNodeConfigurator<SubjectConfig, Subject>,
    override val config: SubjectConfig
) : NavigationNode<Config>() {
    private var subject: Subject? = null

    protected fun createLogAndThrowException(message: String): Nothing {
        val exception = IllegalStateException(message)
        log.e(message, exception)
        throw exception
    }

    override fun onCreate() {
        super.onCreate()
        subject = subjectNavigationNodeConfigurator.create()
    }

    override fun onStart() {
        super.onStart()
        subject ?.let {
            subjectNavigationNodeConfigurator.configure(it, this@SubjectNavigationNode)
        } ?: createLogAndThrowException("Unable to start node due to unexpected absence of subject")
    }

    override fun onResume() {
        super.onResume()
        subject ?.let {
            subjectNavigationNodeConfigurator.place(it, config)
        } ?: createLogAndThrowException("Unable to start node due to unexpected absence of subject")
    }

    override fun onPause() {
        super.onPause()
        subject ?.let {
            subjectNavigationNodeConfigurator.displace(it)
        } ?: createLogAndThrowException("Unable to start node due to unexpected absence of subject")
    }

    override fun onStop() {
        super.onStop()
        subject ?.let {
            subjectNavigationNodeConfigurator.disconfigure(it, config)
        } ?: createLogAndThrowException("Unable to start node due to unexpected absence of subject")
    }

    override fun onDestroy() {
        super.onDestroy()
        subject ?.let {
            subjectNavigationNodeConfigurator.destroy(it)
        } ?: createLogAndThrowException("Unable to start node due to unexpected absence of subject")
        subject = null
    }
}


