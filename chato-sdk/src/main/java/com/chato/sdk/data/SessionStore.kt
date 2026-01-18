package com.chato.sdk.data

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SessionStore {
    private val sessionByApiKey = ConcurrentHashMap<String, String>()

    fun getOrCreateSessionId(apiKey: String): String {
        return sessionByApiKey[apiKey] ?: run {
            val sid = UUID.randomUUID().toString()
            sessionByApiKey[apiKey] = sid
            sid
        }
    }

    fun clear(apiKey: String) {
        sessionByApiKey.remove(apiKey)
    }

    fun clearAll() {
        sessionByApiKey.clear()
    }
}
