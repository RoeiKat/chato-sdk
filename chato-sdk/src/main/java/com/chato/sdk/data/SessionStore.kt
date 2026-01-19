package com.chato.sdk.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object SessionStore {
    private const val PREFS = "chato_session_store"
    private const val KEY_PREFIX = "sid_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getExistingSessionId(apiKey: String): String? {
        return prefs?.getString(KEY_PREFIX + apiKey, null)
    }

    fun getOrCreateSessionId(apiKey: String): String {
        val existing = getExistingSessionId(apiKey)
        if (!existing.isNullOrBlank()) return existing

        val sid = UUID.randomUUID().toString()
        prefs?.edit()?.putString(KEY_PREFIX + apiKey, sid)?.apply()
        return sid
    }

    fun clear(apiKey: String) {
        prefs?.edit()?.remove(KEY_PREFIX + apiKey)?.apply()
    }
}
