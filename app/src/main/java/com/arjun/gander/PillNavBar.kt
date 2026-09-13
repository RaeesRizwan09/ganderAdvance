package com.arjun.gander

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import com.google.android.material.color.MaterialColors

/**
 * Floating pill of four destinations at the bottom of the home screen.
 *
 * Inactive tabs stay an icon. The active one stretches so its name can sit
 * beside it, and the change is a ChangeBounds rather than a swap, so the
 * pill grows into the new tab instead of jumping. Clicks only change which
 * tab is marked open: the destinations themselves are wired by the host later.
 */
class PillNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun interface Listener {
        fun onTabSelected(index: Int)
    }

    var onTabSelected: Listener? = null

    var selectedIndex: Int = TAB_HOME
        private set

    private val tabViews = mutableListOf<View>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipToOutline = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.nav_bar)
        val inflater = LayoutInflater.from(context)
        TABS.forEachIndexed { index, tab ->
            val view = inflater.inflate(R.layout.view_pill_nav_tab, this, false)
            view.findViewById<ImageView>(R.id.tabIcon).setImageResource(tab.icon)
            view.findViewById<TextView>(R.id.tabLabel).setText(tab.label)
            view.contentDescription = context.getString(tab.label)
            view.setOnClickListener { select(index, animate = true, notify = true) }
            addView(view)
            tabViews += view
        }
        applySelection(selectedIndex, animate = false)
    }

    fun select(index: Int, animate: Boolean, notify: Boolean = false) {
        if (index !in tabViews.indices) return
        if (index == selectedIndex && tabViews[index].isSelected) return
        selectedIndex = index
        applySelection(index, animate)
        if (notify) onTabSelected?.onTabSelected(index)
    }

    private fun applySelection(index: Int, animate: Boolean) {
        if (animate) {
            // The host, not this view: ChangeBounds has to see the pill's own
            // width change as well as the tab inside it, and running the
            // transition on the activity root would animate the list too.
            val host = (parent as? ViewGroup) ?: this
            TransitionManager.beginDelayedTransition(
                host,
                TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(
                        ChangeBounds().apply {
                            interpolator = STRETCH
                            duration = STRETCH_MS
                        }
                    )
                    addTransition(
                        Fade().apply {
                            interpolator = STRETCH
                            duration = FADE_MS
                            startDelay = FADE_DELAY_MS
                        }
                    )
                }
            )
        }
        tabViews.forEachIndexed { i, view ->
            val selected = i == index
            view.isSelected = selected
            val label = view.findViewById<TextView>(R.id.tabLabel)
            val icon = view.findViewById<ImageView>(R.id.tabIcon)
            label.visibility = if (selected) VISIBLE else GONE
            val color = MaterialColors.getColor(
                this,
                if (selected) com.google.android.material.R.attr.colorOnPrimaryContainer
                else com.google.android.material.R.attr.colorOnSurfaceVariant
            )
            label.setTextColor(color)
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color))
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = SavedState(super.onSaveInstanceState() ?: AbsSavedState.EMPTY_STATE)
        state.index = selectedIndex
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            select(state.index, animate = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : AbsSavedState {
        var index: Int = TAB_HOME

        constructor(superState: Parcelable) : super(superState)

        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            index = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(index)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.ClassLoaderCreator<SavedState> =
                object : Parcelable.ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(source: Parcel, loader: ClassLoader?) =
                        SavedState(source, loader)

                    override fun createFromParcel(source: Parcel) =
                        SavedState(source, null)

                    override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
                }
        }
    }

    private data class Tab(val label: Int, val icon: Int)

    companion object {
        const val TAB_HOME = 0
        const val TAB_FILE = 1
        const val TAB_SEARCH = 2
        const val TAB_SETTINGS = 3

        private val TABS = listOf(
            Tab(R.string.nav_home, R.drawable.ic_home),
            Tab(R.string.nav_file, R.drawable.ic_file),
            Tab(R.string.nav_search, R.drawable.ic_nav_search),
            Tab(R.string.nav_settings, R.drawable.ic_settings),
        )

        private val STRETCH = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        private const val STRETCH_MS = 380L
        private const val FADE_MS = 220L
        private const val FADE_DELAY_MS = 60L
    }
}
