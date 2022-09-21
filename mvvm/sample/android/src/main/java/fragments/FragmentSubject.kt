package dev.inmo.navigation.mvvm.sample.android.fragments

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlin.reflect.KProperty

abstract class FragmentSubject<Config : Any> : Fragment() {


    open fun configure(config: Config) {
        bundleOf(
            *config::class.members.mapNotNull {
                if (it is KProperty<*>) {
                    it.name to it.getter.call()
                } else {
                    null
                }
            }.toTypedArray()
        )
    }
}
