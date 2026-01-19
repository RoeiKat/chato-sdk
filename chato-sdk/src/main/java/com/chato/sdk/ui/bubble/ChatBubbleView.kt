package com.chato.sdk.ui.bubble

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.chato.sdk.R

class ChatBubbleView(context: Context) : FrameLayout(context) {

    private val bgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(ContextCompat.getColor(context, R.color.chato_primary))
    }

    private var iconImage: ImageView? = null
    private var iconWeb: WebView? = null

    init {
        val size = dp(112)
        layoutParams = LayoutParams(size, size)

        background = bgDrawable
        elevation = dp(8).toFloat()
        clipToOutline = true

        setDefaultIcon()
    }

    fun setBubbleColor(colorInt: Int) {
        bgDrawable.setColor(colorInt)
        invalidate()
    }

    fun setIconSvgOrDefault(svgOrNull: String?) {
        if (svgOrNull.isNullOrBlank()) {
            setDefaultIcon()
            return
        }
        setSvgIcon(svgOrNull)
    }

    private fun setDefaultIcon() {
        // Remove svg webview if exists
        iconWeb?.let { removeView(it) }
        iconWeb = null

        val icon = iconImage ?: ImageView(context).also { iconImage = it }
        icon.apply {
            setImageResource(R.drawable.ic_chato_chat)
            setColorFilter(ContextCompat.getColor(context, R.color.chato_black))
            layoutParams = LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.CENTER
            }
        }

        if (icon.parent == null) addView(icon)
    }

    private fun setSvgIcon(svg: String) {
        // Remove image if exists
        iconImage?.let { removeView(it) }

        val wv = iconWeb ?: WebView(context).also { iconWeb = it }
        wv.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            layoutParams = LayoutParams(dp(60), dp(60)).apply {
                gravity = Gravity.CENTER
            }
        }

        if (wv.parent == null) addView(wv)

        // Make sure SVG has a viewport; user may upload any SVG, we just embed it.
        val html = """
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <style>
                  body, html { margin:0; padding:0; background:transparent; }
                  svg { width:100%; height:100%; }
                </style>
              </head>
              <body>
                $svg
              </body>
            </html>
        """.trimIndent()

        wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun dp(v: Int): Int {
        val density = resources.displayMetrics.density
        return (v * density).toInt()
    }
}
