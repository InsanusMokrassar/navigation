package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import dev.inmo.navigation.mvvm.sample.android.R

class TextFragment : FragmentSubject<AndroidNodeConfig.TextConfig>() {
    protected val text: String by argument()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false).apply {
            findViewById<TextView>(R.id.fragment_text_view) ?.text = this@TextFragment.text
            findViewById<View>(R.id.fragment_text_increase).setOnClickListener {
                node ?.chain ?.push(
                    AndroidNodeConfig.TextConfig(
                        id,
                        text + "X"
                    )
                )
            }
            findViewById<View>(R.id.fragment_text_back).setOnClickListener {
                node ?.chain ?.pop()
            }
        }
    }
}