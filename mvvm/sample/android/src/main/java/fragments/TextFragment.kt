package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import dev.inmo.micro_utils.common.argumentOrThrow
import dev.inmo.micro_utils.common.findViewsByTag
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationStateChange
import dev.inmo.navigation.core.extensions.*
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.AndroidNodeConfig
import dev.inmo.navigation.mvvm.sample.android.R
import kotlinx.coroutines.flow.*

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

            suspend fun enableChainListening(chain: NavigationChain<AndroidNodeConfig>) {
                var layout: View? = null

                val subscriptionJob = chain.stackFlow.subscribeSafelyWithoutExceptions(scope) {
                    if (it.isNotEmpty() && it.first().config.viewTag == layout ?.tag) {
                        return@subscribeSafelyWithoutExceptions
                    }

                    layout ?.let { removeView(it) }

                    layout = addFrameLayout(it.first().config.viewTag) ?: return@subscribeSafelyWithoutExceptions
                }

                node.onChainRemovedFlow.flatten().dropWhile { it.value != chain }.take(1).collect()
                subscriptionJob.cancel()
            }

            node.onChainAddedFlow.flatten().subscribeSafelyWithoutExceptions(scope) { (_, chain) ->
                enableChainListening(chain)
            }

//            scope.launchSafelyWithoutExceptions {
//                node.subchainsFlow.value.forEach {
//                    enableChainListening(it)
//                }
//            }

            findViewById<View>(R.id.fragment_text_subchain).setOnClickListener {
                val subViewTag = "${viewTag}subview${node.subchainsFlow.value.size}"

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
