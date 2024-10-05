package dev.inmo.navigation.core

import kotlin.reflect.KClass

fun interface NavigationNodeFactory<Base> {
    fun createNode(
        navigationChain: NavigationChain<Base>,
        config: Base
    ): NavigationNode<out Base, Base>?

    open class Typed<T : Base, Base : Any?>(
        private val kClass: KClass<*>,
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
            inline operator fun <reified T : Base, Base : Any?> invoke(
                noinline block: (navigationChain: NavigationChain<Base>, config: T) -> NavigationNode<T, Base>
            ) = Typed<T, Base>(
                kClass = T::class,
                block
            )
        }
    }

    open class DefaultAggregator<Base>(
        private val factories: List<NavigationNodeFactory<Base>>,
    ) : NavigationNodeFactory<Base> {
        override fun createNode(navigationChain: NavigationChain<Base>, config: Base): NavigationNode<out Base, Base>? {
            return factories.firstNotNullOfOrNull { it.createNode(navigationChain, config) }
        }
    }
}
