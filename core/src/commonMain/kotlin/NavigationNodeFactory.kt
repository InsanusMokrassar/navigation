package dev.inmo.navigation.core

import kotlin.reflect.KClass

fun interface NavigationNodeFactory<Base> {
    fun createNode(
        navigationChain: NavigationChain<Base>,
        config: Base
    ): NavigationNode<out Base, Base>?

    companion object {
        open class Typed<Base : Any, T : Base>(
            private val kClass: KClass<T>,
            private val block: (navigationChain: NavigationChain<Base>, config: T) -> NavigationNode<T, Base>
        ) : NavigationNodeFactory<Base> {
            override fun createNode(
                navigationChain: NavigationChain<Base>,
                config: Base
            ): NavigationNode<out Base, Base>? {
                return if (kClass.isInstance(config)) {
                    block(navigationChain, config as T)
                } else {
                    null
                }
            }

            companion object {
                inline operator fun <Base : Any, reified T : Base> invoke(
                    noinline block: (navigationChain: NavigationChain<Base>, config: T) -> NavigationNode<T, Base>
                ) = Typed<Base, T>(
                    kClass = T::class,
                    block
                )
            }
        }
    }
}
