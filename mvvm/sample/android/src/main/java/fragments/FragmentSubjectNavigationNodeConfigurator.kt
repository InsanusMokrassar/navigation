package dev.inmo.navigation.mvvm.sample.android.fragments

import dev.inmo.navigation.core.subject.SubjectNavigationNodeConfigurator
import kotlin.reflect.KClass

class FragmentSubjectNavigationNodeConfigurator<Config>(
    private val fragmentKlass: KClass<out FragmentSubject>
) : SubjectNavigationNodeConfigurator<Config, FragmentSubject> {
    override fun create(): FragmentSubject = fragmentKlass.objectInstance ?: fragmentKlass.constructors.first {
        it.parameters.isEmpty()
    }.call()

    override fun configure(subject: FragmentSubject, config: Config) {
        TODO("Not yet implemented")
    }

    override fun place(subject: FragmentSubject) {
        TODO("Not yet implemented")
    }

    override fun displace(subject: FragmentSubject) {
        TODO("Not yet implemented")
    }

    override fun disconfigure(subject: FragmentSubject, config: Config) {
        TODO("Not yet implemented")
    }

    override fun destroy(subject: FragmentSubject) {
        TODO("Not yet implemented")
    }
}
