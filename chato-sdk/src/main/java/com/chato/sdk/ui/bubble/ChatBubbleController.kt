package com.chato.sdk.ui.bubble

import android.app.Activity
import android.graphics.Point
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.chato.sdk.Chato

class ChatBubbleController(
    private val activity: Activity,
    private val onClick: () -> Unit
) {
    private var bubbleView: ChatBubbleView? = null

    fun attach() {
        if (bubbleView != null) return

        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val container = ensureOverlayContainer(root)

        val v = ChatBubbleView(activity).apply {
            setOnClickListener { onClick() }

            // Apply remote theme (or fallbacks)
            setBubbleColor(Chato.resolveBubbleBgColor(activity))
            setIconSvgOrDefault(Chato.resolveIconSvgOrNull())
        }

        // Default position: middle-left
        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            leftMargin = dp(12)
            topMargin = (size.y / 2) - dp(28)
        }

        v.layoutParams = lp
        enableDrag(v)

        container.addView(v)
        bubbleView = v
    }

    fun detach() {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val container = root.findViewWithTag<FrameLayout>("chato_bubble_container")
        bubbleView?.let { container?.removeView(it) }
        bubbleView = null
    }

    private fun ensureOverlayContainer(root: ViewGroup): FrameLayout {
        val existing = root.findViewWithTag<FrameLayout>("chato_bubble_container")
        if (existing != null) return existing

        val container = FrameLayout(activity).apply {
            tag = "chato_bubble_container"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }
        root.addView(container)
        return container
    }

    private fun enableDrag(view: View) {
        var dX = 0f
        var dY = 0f
        var downRawX = 0f
        var downRawY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dX = v.x - downRawX
                    dY = v.y - downRawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    if (kotlin.math.abs(event.rawX - downRawX) > dp(4) ||
                        kotlin.math.abs(event.rawY - downRawY) > dp(4)
                    ) {
                        isDragging = true
                    }
                    v.x = newX
                    v.y = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int {
        val density = activity.resources.displayMetrics.density
        return (v * density).toInt()
    }
}
