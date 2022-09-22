package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import dev.inmo.navigation.mvvm.sample.android.R

class TextFragment : NodeFragment<AndroidNodeConfig>() {
    protected val text: String by argument()
    protected val viewId: Int by argument()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false).apply {
            findViewById<TextView>(R.id.fragment_text_view) ?.text = this@TextFragment.text
            findViewById<View>(R.id.fragment_text_increase).setOnClickListener {
                node.chain.push(
                    AndroidNodeConfig.TextConfig(
                        viewId,
                        "${text}X"
                    )
                )
            }
            findViewById<View>(R.id.fragment_text_back).setOnClickListener {
                node.chain.pop()
            }
            findViewById<View>(R.id.fragment_text_subchain).setOnClickListener {
                node.createSubChain(
                    AndroidNodeConfig.TextConfig(
                        R.id.fragment_text_subchain_container,
                        "Sub$text"
                    )
                )
            }
        }
    }
}
