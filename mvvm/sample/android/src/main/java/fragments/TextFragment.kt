package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import dev.inmo.micro_utils.common.argumentOrThrow
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import dev.inmo.navigation.mvvm.sample.android.R

class TextFragment : NodeFragment<AndroidNodeConfig>() {
    protected val text: String by argumentOrThrow()
    protected val viewTag: String by argumentOrThrow()
    protected val subViewTag: String by lazy {
        "${viewTag}subview"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false).apply {
            findViewById<View>(R.id.fragment_text_subchain_container) ?.tag = subViewTag
            findViewById<TextView>(R.id.fragment_text_view) ?.text = this@TextFragment.text
            findViewById<View>(R.id.fragment_text_increase).setOnClickListener {
                node.chain.push(
                    AndroidNodeConfig.TextConfig(
                        viewTag,
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
                        subViewTag,
                        "Sub$text"
                    )
                )
            }
        }
    }
}
