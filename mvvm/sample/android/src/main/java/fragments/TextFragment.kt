package dev.inmo.navigation.mvvm.sample.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.children
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.common.argumentOrThrow
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.extensions.*
import dev.inmo.navigation.core.fragments.NodeFragment
import dev.inmo.navigation.mvvm.sample.android.SampleConfig
import dev.inmo.navigation.mvvm.sample.android.R
import kotlinx.coroutines.flow.*

class TextFragment : NodeFragment<SampleConfig>() {
    protected val text: String by argumentOrThrow()
    protected val viewTag: String by argumentOrThrow()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return view ?: (inflater.inflate(R.layout.fragment_text, container, false) as ViewGroup)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as? ViewGroup) ?.apply {
            findViewById<TextView>(R.id.fragment_text_view) ?.text = this@TextFragment.text
            findViewById<View>(R.id.fragment_text_forward).setOnClickListener {
                node.chain.push(
                    SampleConfig.TextConfig(
                        viewTag,
                        "${text}${getString(R.string.forward)}"
                    )
                )
            }
            findViewById<View>(R.id.fragment_text_back).setOnClickListener {
                node.chain.pop()
            }

            findViewById<View>(R.id.fragment_text_subchain).setOnClickListener {
                val subViewTag = "${viewTag}subview${node.subchainsFlow.value.size}"

                node.createSubChain(
                    SampleConfig.TextConfig(
                        subViewTag,
                        "$text${getString(R.string.subchain)}${node.subchainsFlow.value.size}"
                    )
                )
            }
        } ?: return
    }

    override fun onResume() {
        super.onResume()

        suspend fun addFrameLayout(tag: String): ViewGroup? {
            val newView = FrameLayout(
                context ?: return null,
            ).apply {
                this.tag = tag
                id = View.generateViewId()
                visibility = View.VISIBLE
            }
            (view as? ViewGroup) ?.apply {
                doInUI {
                    addView(newView)
                }
                children.forEach {
                    this@TextFragment.logger.d(it)
                }
            }

            return newView
        }

        suspend fun doChainListening(chain: NavigationChain<SampleConfig>) {
            var layout: View? = null

            val subscriptionJob = chain.stackFlow.subscribeSafelyWithoutExceptions(scope) {
                val firstConfig = it.firstOrNull() ?.config ?: return@subscribeSafelyWithoutExceptions
                val viewAsViewGroup = (view as? ViewGroup) ?: return@subscribeSafelyWithoutExceptions

                if (it.isNotEmpty() && firstConfig.viewTag == layout ?.tag) {
                    return@subscribeSafelyWithoutExceptions
                }
                val viewByTag = viewAsViewGroup.findViewWithTag<View>(firstConfig.viewTag)

                layout ?.let { viewAsViewGroup.removeView(it) }

                layout = viewByTag ?: addFrameLayout(firstConfig.viewTag) ?: return@subscribeSafelyWithoutExceptions
            }

            node.onChainRemovedFlow.flatten().filter { it.value == chain }.take(1).collect()
            subscriptionJob.cancel()
            (view as? ViewGroup) ?.removeView(layout ?: return)
        }

        node.onChainAddedFlow.flatten().subscribeSafelyWithoutExceptions(scope) { (_, chain) ->
            scope.launchSafelyWithoutExceptions { doChainListening(chain) }
        }
    }
}
