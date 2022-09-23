package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import dev.inmo.micro_utils.common.argumentOrThrow
import dev.inmo.micro_utils.coroutines.flatten
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.navigation.core.extensions.onChainAddedFlow
import dev.inmo.navigation.core.extensions.onNodeRemovedFlow
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import dev.inmo.navigation.mvvm.sample.android.R

class TextFragment : NodeFragment<AndroidNodeConfig>() {
    protected val text: String by argumentOrThrow()
    protected val viewTag: String by argumentOrThrow()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return (inflater.inflate(R.layout.fragment_text, container, false) as ViewGroup).apply {
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

            fun addFrameLayout(tag: String): View? {
                val view = FrameLayout(context ?: return null).apply {
                    this.tag = tag
                    id = View.generateViewId()
                }
                addView(view)
                return view
            }

            findViewById<View>(R.id.fragment_text_subchain).setOnClickListener {
                val subViewTag = "${viewTag}subview${node.chain.stackFlow.value.size}"
                val view = addFrameLayout(subViewTag) ?: return@setOnClickListener

                addView(view)

                node.createSubChain(
                    AndroidNodeConfig.TextConfig(
                        subViewTag,
                        "Sub$text"
                    )
                ) ?.second ?.let {
                    it.onNodeRemovedFlow.flatten().subscribeSafelyWithoutExceptions() { _ ->
                        if (it.stackFlow.value.isEmpty()) {
                            removeView(view)
                        }
                    }
                }
            }
        }
    }
}
