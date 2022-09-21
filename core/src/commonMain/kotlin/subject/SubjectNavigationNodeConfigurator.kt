package dev.inmo.navigation.core.subject

interface SubjectNavigationNodeConfigurator<Config, Subject> {
    fun create(): Subject
    fun configure(subject: Subject, config: Config)
    fun place(subject: Subject)

    fun displace(subject: Subject)
    fun disconfigure(subject: Subject, config: Config)
    fun destroy(subject: Subject)
}
