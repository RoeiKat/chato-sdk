package com.chato.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.chato.sdk.data.ChatoConfig
import com.chato.sdk.data.RemoteConfigStore
import com.chato.sdk.data.SessionStore
import com.chato.sdk.internal.ChatoTaskRemovedService
import com.chato.sdk.net.ChatoApi
import com.chato.sdk.net.RetrofitFactory
import com.chato.sdk.net.dto.SdkConfigRes
import com.chato.sdk.realtime.FirebaseRealtime
import com.chato.sdk.ui.ChatActivity
import com.chato.sdk.ui.bubble.ChatBubbleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object Chato {

    private const val BASE_URL = "https://chato-backend.onrender.com/"

    // ✅ safe to store
    private var application: Application? = null

    private var config: ChatoConfig? = null
    private var api: ChatoApi? = null
    private var bubbleController: ChatBubbleController? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Gate UI by apiKey validity
    @Volatile private var hasValidatedKey: Boolean = false
    @Volatile private var isApiKeyValid: Boolean = false
    @Volatile private var isValidatingKey: Boolean = false

    // Remember activity while validating
    @Volatile private var pendingAttachActivityRef: WeakReference<Activity>? = null

    fun init(context: Context, apiKey: String) {
        val app = context.applicationContext as Application
        application = app

        SessionStore.init(app)

        config = ChatoConfig(apiKey = apiKey.trim())

        api = RetrofitFactory.create(baseUrl = BASE_URL)
            .create(ChatoApi::class.java)

        FirebaseRealtime.init(context = app)

        ensureTaskRemovedServiceState()
        validateApiKeySilently()
    }

    fun attach(activity: Activity) {
        requireConfig()

        if (hasValidatedKey && !isApiKeyValid) {
            android.util.Log.w("CHATO", "API key invalid - bubble will not attach")
            return
        }

        if (!hasValidatedKey) {
            pendingAttachActivityRef = WeakReference(activity)
            if (!isValidatingKey) {
                android.util.Log.d("CHATO", "Validating API key - bubble attach skipped")
                validateApiKeySilently()
            }
            return
        }

        attachBubble(activity)
    }

    fun detach() {
        bubbleController?.detach()
        bubbleController = null
    }

    fun getApiKey(): String = requireConfig().apiKey
    fun getApiKeyOrNull(): String? = config?.apiKey

    fun getApi(): ChatoApi =
        api ?: error("Chato.init(context, apiKey) was not called")

    fun getApiOrNull(): ChatoApi? = api

    @Suppress("unused") // public API for callers
    fun getCachedRemoteConfig(): SdkConfigRes? = RemoteConfigStore.get()

    fun refreshRemoteConfig(onDone: ((SdkConfigRes?) -> Unit)? = null) {
        val apiKey = getApiKey()
        val api = getApi()

        scope.launch {
            try {
                val cfg = api.getConfig(apiKey = apiKey)
                RemoteConfigStore.set(cfg)
                isApiKeyValid = true
                hasValidatedKey = true
                onDone?.invoke(cfg)

                pendingAttachActivityRef?.get()?.let { act ->
                    act.runOnUiThread { attach(act) }
                }
            } catch (e: Exception) {
                android.util.Log.w("CHATO", "getConfig failed: ${e.message}")
                RemoteConfigStore.clear()
                isApiKeyValid = false
                hasValidatedKey = true
                onDone?.invoke(null)
            }
        }
    }

    fun resolveBubbleBgColor(context: Context): Int {
        val remote = RemoteConfigStore.get()?.theme?.bubbleBg.orEmpty().trim()
        val parsed = remote.toColorIntOrNull()
        return parsed ?: ContextCompat.getColor(context, R.color.chato_primary)
    }

    fun resolvePrimaryColor(context: Context): Int {
        val remote = RemoteConfigStore.get()?.theme?.primary.orEmpty().trim()
        val parsed = remote.toColorIntOrNull()
        return parsed ?: ContextCompat.getColor(context, R.color.chato_primary)
    }

    fun resolveIconSvgOrNull(): String? {
        val svg = RemoteConfigStore.get()?.theme?.iconSvg.orEmpty().trim()
        return svg.ifBlank { null }
    }

    fun resolveSupportTitle(): String {
        val raw = RemoteConfigStore.get()?.theme?.title
        val cleaned = raw?.toString()?.trim().orEmpty()
        return if (cleaned.isBlank()) "Support" else cleaned
    }

    fun getExistingSessionIdOrNull(): String? {
        val key = config?.apiKey ?: return null
        return SessionStore.getExistingSessionId(key)
    }

    fun getOrCreateSessionId(): String {
        val key = requireConfig().apiKey
        val sid = SessionStore.getOrCreateSessionId(key)
        ensureTaskRemovedServiceState()
        return sid
    }

    @Suppress("unused") // public API for callers
    fun clearSession() {
        val key = requireConfig().apiKey
        SessionStore.clear(key)
        ensureTaskRemovedServiceState()
    }

    // ---- internals ----

    private fun ensureTaskRemovedServiceState() {
        val app = application ?: return
        val key = config?.apiKey ?: return
        val sid = SessionStore.getExistingSessionId(key)

        val intent = Intent(app, ChatoTaskRemovedService::class.java)
        if (!sid.isNullOrBlank()) {
            app.startService(intent)
        } else {
            app.stopService(intent)
        }
    }

    private fun attachBubble(activity: Activity) {
        bubbleController = bubbleController ?: ChatBubbleController(
            activity = activity,
            onClick = {
                if (!isApiKeyValid) {
                    android.util.Log.w("CHATO", "API key invalid - chat not opened")
                    return@ChatBubbleController
                }
                activity.startActivity(Intent(activity, ChatActivity::class.java))
            }
        )
        bubbleController?.attach()
    }

    private fun validateApiKeySilently() {
        val apiKey = getApiKey()
        val api = getApi()

        isValidatingKey = true

        scope.launch {
            try {
                val cfg = api.getConfig(apiKey = apiKey)
                RemoteConfigStore.set(cfg)
                isApiKeyValid = true
            } catch (e: Exception) {
                android.util.Log.w("CHATO", "API key validation failed: ${e.message}")
                RemoteConfigStore.clear()
                isApiKeyValid = false
            } finally {
                hasValidatedKey = true
                isValidatingKey = false

                val act = pendingAttachActivityRef?.get()
                if (isApiKeyValid && act != null && !act.isFinishing && !act.isDestroyed) {
                    act.runOnUiThread { attach(act) }
                }
            }
        }
    }

    private fun String.toColorIntOrNull(): Int? {
        if (isBlank()) return null
        return try {
            // accepts "#RRGGBB" or "#AARRGGBB"
            this.toColorInt()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun requireConfig(): ChatoConfig =
        config ?: error("Chato.init(context, apiKey) was not called")
}
