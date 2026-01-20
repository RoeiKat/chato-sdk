package com.chato.sdk.ui.bubble

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.chato.sdk.R

class ChatBubbleView(context: Context) : FrameLayout(context) {

    private val bubbleSizeDp = 80
    private val iconPaddingDp = 18

    private val SVG_SCALE = 1.15

    private val bgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(ContextCompat.getColor(context, R.color.chato_primary))
    }

    private var iconImage: ImageView? = null
    private var iconWeb: WebView? = null

    init {
        val size = dp(bubbleSizeDp)
        layoutParams = LayoutParams(size, size)

        background = bgDrawable
        elevation = dp(8).toFloat()
        clipToOutline = true

        setPadding(dp(iconPaddingDp), dp(iconPaddingDp), dp(iconPaddingDp), dp(iconPaddingDp))

        setDefaultIcon()
    }

    fun setBubbleColor(colorInt: Int) {
        bgDrawable.setColor(colorInt)
        invalidate()
    }

    fun setIconSvgOrDefault(svgOrNull: String?) {
        if (svgOrNull.isNullOrBlank()) setDefaultIcon() else setSvgIcon(svgOrNull)
    }

    private fun setDefaultIcon() {
        iconWeb?.let { removeView(it) }
        iconWeb = null

        val icon = iconImage ?: ImageView(context).also { iconImage = it }
        icon.apply {
            setImageResource(R.drawable.ic_chato_chat)
            setColorFilter(ContextCompat.getColor(context, R.color.chato_black))
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        }

        if (icon.parent == null) addView(icon)
    }

    private fun setSvgIcon(svg: String) {
        iconImage?.let { removeView(it) }

        val wv = iconWeb ?: WebView(context).also { iconWeb = it }
        wv.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            webViewClient = WebViewClient()

            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.domStorageEnabled = false

            // Fill bubble inner area; bubble padding handles size.
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }

            // ✅ IMPORTANT: WebView steals taps — forward them to the bubble click listener.
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        // This triggers the ChatBubbleView's click listener set by the controller
                        this@ChatBubbleView.performClick()
                        true
                    }
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> true
                    else -> true
                }
            }

            // Prevent long-press selection/context menu
            setOnLongClickListener { true }
            isLongClickable = false

            clearCache(true)
            clearHistory()
        }

        if (wv.parent == null) addView(wv)

        val html = """
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <style>
                  html, body {
                    margin: 0; padding: 0;
                    width: 100%; height: 100%;
                    background: transparent;
                    overflow: hidden;
                  }
                  .wrap {
                    width: 100%; height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                  }
                  svg {
                    width: auto !important;
                    height: auto !important;
                    max-width: 100% !important;
                    max-height: 100% !important;
                    display: block;
                    transform: scale(${'$'}{SVG_SCALE});
                    transform-origin: center center;
                  }
                </style>
              </head>
              <body>
                <div class="wrap">
                  $svg
                </div>
              </body>
            </html>
        """.trimIndent()

        wv.loadDataWithBaseURL("https://chato.local/", html, "text/html", "UTF-8", null)
    }

    // Accessibility: keep performClick happy
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dp(v: Int): Int {
        val density = resources.displayMetrics.density
        return (v * density).toInt()
    }
}
