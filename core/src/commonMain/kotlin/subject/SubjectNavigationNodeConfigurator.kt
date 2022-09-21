package dev.inmo.navigation.core.subject

import dev.inmo.navigation.core.NavigationNode

interface SubjectNavigationNodeConfigurator<Config, Subject> {
    fun create(): Subject
    fun configure(subject: Subject, node: NavigationNode<Config>)
    fun place(subject: Subject, config: Config)

    fun displace(subject: Subject)
    fun disconfigure(subject: Subject, config: Config)
    fun destroy(subject: Subject)
}
