package com.chato.sdk.ui.bubble

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.chato.sdk.R

class ChatBubbleView(context: Context) : FrameLayout(context) {

    init {
        val size = dp(112)
        layoutParams = LayoutParams(size, size)

        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.chato_primary))
        }
        elevation = dp(8).toFloat()
        clipToOutline = true

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_chato_chat)
            setColorFilter(ContextCompat.getColor(context, R.color.chato_black))
            layoutParams = LayoutParams(dp(52), dp(52)).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        addView(icon)
    }

    private fun dp(v: Int): Int {
        val density = resources.displayMetrics.density
        return (v * density).toInt()
    }
}
