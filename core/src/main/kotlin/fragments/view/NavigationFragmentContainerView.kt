package dev.inmo.navigation.core.fragments.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import dev.inmo.navigation.core.R
import dev.inmo.navigation.core.navigationTagKey

class NavigationFragmentContainerView(
    context: Context,
    attrs: AttributeSet?,
    @AttrRes defStyleAttr: Int,
    defStyleRes: Int
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var navigationTag: String?
        get() = getTag(navigationTagKey) as? String
        set(value) {
            setTag(navigationTagKey, value)
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, 0)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NavigationFragmentTargetView,
            0,
            0
        ).apply {
            runCatching {
                navigationTag = getString(R.styleable.NavigationFragmentTargetView_navigationTag)
            }
            recycle()
        }
    }


}
