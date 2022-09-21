package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.fragment.app.FragmentManager
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.subject.SubjectNavigationNodeConfigurator
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import kotlin.reflect.KClass

class FragmentSubjectNavigationNodeConfigurator<Config : AndroidNodeConfig>(
    private val fragmentKlass: KClass<out FragmentSubject<Config>>,
    private val fragmentManager: FragmentManager
) : SubjectNavigationNodeConfigurator<Config, FragmentSubject<Config>> {
    override fun create(): FragmentSubject<Config> = fragmentKlass.objectInstance ?: fragmentKlass.constructors.first {
        it.parameters.isEmpty()
    }.call()

    override fun configure(subject: FragmentSubject<Config>, node: NavigationNode<Config>) {
        subject.configure(node)
    }

    override fun place(subject: FragmentSubject<Config>, config: Config) {
        fragmentManager.beginTransaction().runCatching {
            replace(config.viewId, subject)
            commit()
        }
    }

    override fun displace(subject: FragmentSubject<Config>) {
        fragmentManager.beginTransaction().apply {
            runCatching {
                remove(subject)
            }.onSuccess {
                commit()
            }
        }
    }

    override fun disconfigure(subject: FragmentSubject<Config>, config: Config) {
        // do nothing
    }

    override fun destroy(subject: FragmentSubject<Config>) {
        // do nothing
    }
}
