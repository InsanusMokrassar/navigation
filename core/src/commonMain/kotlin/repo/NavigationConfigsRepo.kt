package dev.inmo.navigation.core.repo

interface NavigationConfigsRepo<T> {
    fun save(holder: ConfigHolder<T>)
    fun get(): ConfigHolder<T>?

    class InMemory<T>(
        initial: ConfigHolder<T>? = null
    ) : NavigationConfigsRepo<T> {
        private var savedHolder: ConfigHolder<T>? = initial
        override fun save(holder: ConfigHolder<T>) {
            savedHolder = holder
        }

        override fun get(): ConfigHolder<T>? = savedHolder

        companion object {
            val Global = InMemory<Any>()
        }
    }
}
