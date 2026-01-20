package com.chato.sdk.ui.bubble

import android.app.Activity
import android.graphics.Point
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.chato.sdk.Chato
import kotlin.math.abs

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

        // Do NOT replace layoutParams with WRAP_CONTENT.
        // Use the view's own layout params (it sets exact bubble size), only set gravity + margins.
        val display = activity.windowManager.defaultDisplay
        val screen = Point()
        display.getSize(screen)

        val lp = (v.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(v.measuredWidth, v.measuredHeight)

        lp.gravity = Gravity.START or Gravity.TOP
        lp.leftMargin = dp(12)
        lp.topMargin = (screen.y / 2) - dp(80) // roughly center-ish; adjust if you want
        v.layoutParams = lp

        enableDragAndSnap(v)

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

    private fun enableDragAndSnap(view: View) {
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

                    if (abs(event.rawX - downRawX) > dp(4) || abs(event.rawY - downRawY) > dp(4)) {
                        isDragging = true
                    }

                    v.x = newX
                    v.y = newY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                        return@setOnTouchListener true
                    }

                    // SNAP to left/right so it never stays in the middle
                    val parent = v.parent as? ViewGroup
                    val parentW = parent?.width ?: 0
                    val parentH = parent?.height ?: 0
                    val margin = dp(12).toFloat()

                    val bubbleCenterX = v.x + (v.width / 2f)
                    val snapRight = parentW > 0 && bubbleCenterX > (parentW / 2f)

                    val targetX = if (snapRight) {
                        parentW.toFloat() - v.width - margin
                    } else {
                        margin
                    }

                    val minY = margin
                    val maxY = if (parentH > 0) parentH.toFloat() - v.height - margin else v.y
                    val targetY = v.y.coerceIn(minY, maxY)

                    v.animate()
                        .x(targetX)
                        .y(targetY)
                        .setDuration(160)
                        .start()

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
