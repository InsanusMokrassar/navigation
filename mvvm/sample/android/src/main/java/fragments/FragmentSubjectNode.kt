package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.fragment.app.FragmentActivity
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.subject.SubjectNavigationNode
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import kotlin.reflect.KClass

fun <BaseConfig: AndroidNodeConfig, Config: BaseConfig> FragmentActivity.FragmentSubjectNode(
    navigationChain: NavigationChain<BaseConfig>,
    fragmentKClass: KClass<out FragmentSubject<Config>>,
    config: Config
) = SubjectNavigationNode(
    navigationChain,
    FragmentSubjectNavigationNodeConfigurator(
        fragmentKClass,
        supportFragmentManager
    ),
    config
)
