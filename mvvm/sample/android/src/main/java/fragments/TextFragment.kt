package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import dev.inmo.navigation.mvvm.sample.android.R

class TextFragment : Fragment() {
    val text: String?
        get() = arguments ?.getString("text")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false).apply {
            findViewById<TextView>(R.id.fragment_text_view) ?.text = this@TextFragment.text ?: ""
        }
    }

    companion object {
        operator fun invoke(text: String) = TextFragment().apply {
            arguments = bundleOf(
                "text" to text
            )
        }
    }
}
