package dev.inmo.navigation.mvvm.utils

import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.java.KoinJavaComponent
import kotlin.reflect.KClass

fun <T> inject(
    qualifier: Qualifier? = null,
    kClassFactory: () -> KClass<*>,
    parameters: ParametersDefinition? = null
): Lazy<T> {
    return lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        KoinJavaComponent.get(kClassFactory().java, qualifier, parameters)
    }
}
