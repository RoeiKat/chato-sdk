package com.chato.sdk.realtime

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseRealtime {
    private var initialized = false

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun init(context: Context) {
        if (initialized) return

        val options = FirebaseOptions.Builder()
            .setApplicationId("1:897815588911:android:94a0c3c4d05ad3126696da")
            .setApiKey("AIzaSyD3j64wev21MvAMPFM53WL0n8EZ6AEX-A4")
            .setDatabaseUrl("https://chato-2b0d6-default-rtdb.europe-west1.firebasedatabase.app")
            .build()

        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, options)
        }

        initialized = true
    }

    /**
     * Ensures FirebaseAuth is non-null before we subscribe to RTDB.
     * Calls onReady when auth is available.
     */
    fun ready(onReady: () -> Unit, onError: (Exception) -> Unit = {}) {
        val user = auth.currentUser
        if (user != null) {
            onReady()
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun db(): FirebaseDatabase = FirebaseDatabase.getInstance()
}
