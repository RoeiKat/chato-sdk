package com.chato.sdk.internal

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.chato.sdk.Chato
import com.chato.sdk.data.SessionStore
import com.chato.sdk.net.dto.SendMessageReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatoTaskRemovedService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val apiKey = Chato.getApiKeyOrNull()
        if (apiKey.isNullOrBlank()) {
            stopSelf()
            return
        }

        val sid = SessionStore.getExistingSessionId(apiKey)
        if (sid.isNullOrBlank()) {
            stopSelf()
            return
        }

        // ✅ IMPORTANT: clear session FIRST (synchronously) so next launch ALWAYS starts a new conversation
        SessionStore.clear(apiKey)

        // Best-effort send (unreliable by design)
        scope.launch {
            try {
                Chato.getApiOrNull()?.sendMessage(
                    apiKey = apiKey,
                    body = SendMessageReq(
                        text = "Customer left the conversation",
                        sessionId = sid,
                        from = "customer"
                    )
                )
            } catch (_: Exception) {
                // ignore
            } finally {
                stopSelf()
            }
        }

        super.onTaskRemoved(rootIntent)
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, ChatoTaskRemovedService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatoTaskRemovedService::class.java))
        }
    }
}
