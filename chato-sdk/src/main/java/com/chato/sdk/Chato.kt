package com.chato.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.chato.sdk.data.ChatoConfig
import com.chato.sdk.data.SessionStore
import com.chato.sdk.net.ChatoApi
import com.chato.sdk.net.RetrofitFactory
import com.chato.sdk.realtime.FirebaseRealtime
import com.chato.sdk.ui.ChatActivity
import com.chato.sdk.ui.bubble.ChatBubbleController

object Chato {

    private const val BASE_URL = "http://10.0.2.2:3000/" // <-- change once, inside SDK

    private var config: ChatoConfig? = null
    private var api: ChatoApi? = null
    private var bubbleController: ChatBubbleController? = null

    /**
     * Developer-facing init:
     * Only needs apiKey.
     */
    fun init(context: Context, apiKey: String) {
        val appCtx = context.applicationContext

        config = ChatoConfig(apiKey = apiKey.trim())

        api = RetrofitFactory.create(baseUrl = BASE_URL)
            .create(ChatoApi::class.java)

        FirebaseRealtime.init(context = appCtx)

        com.chato.sdk.data.SessionStore.getOrCreateSessionId(config!!.apiKey)
    }


    fun attach(activity: Activity) {
        requireConfig()
        bubbleController = bubbleController ?: ChatBubbleController(
            activity = activity,
            onClick = {
                activity.startActivity(Intent(activity, ChatActivity::class.java))
            }
        )
        bubbleController?.attach()
    }

    fun detach() {
        bubbleController?.detach()
        bubbleController = null
    }

    fun getApiKey(): String = requireConfig().apiKey

    fun getApi(): ChatoApi =
        api ?: error("Chato.init(context, apiKey) was not called")

    fun getSessionId(): String {
        val cfg = requireConfig()
        return com.chato.sdk.data.SessionStore.getOrCreateSessionId(cfg.apiKey)
    }


    private fun requireConfig(): ChatoConfig =
        config ?: error("Chato.init(context, apiKey) was not called")
}
