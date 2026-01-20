package com.chato.sdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chato.sdk.Chato
import com.chato.sdk.databinding.ActivityChatoChatBinding
import com.chato.sdk.net.dto.SendMessageReq
import com.chato.sdk.net.dto.SdkConfigRes
import com.chato.sdk.realtime.FirebaseRealtime
import com.chato.sdk.ui.model.ChatoMessage
import kotlinx.coroutines.launch
import android.content.res.ColorStateList
import android.graphics.Color


class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatoChatBinding
    private val adapter = ChatAdapter()

    private var msgsQuery: com.google.firebase.database.Query? = null
    private var childListener: com.google.firebase.database.ChildEventListener? = null
    private val seenMessageIds = HashSet<String>()

    // ---- Prechat state ----
    private enum class PrechatStep { WAIT_A1, WAIT_A2, DONE }

    private var step: PrechatStep = PrechatStep.WAIT_A1
    private var cfg: SdkConfigRes? = null

    // Buffer transcript to send AFTER Q3
    private val buffered = mutableListOf<Pair<String, String>>()

    private fun nowMs(): Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatoChatBinding.inflate(layoutInflater)

        supportActionBar?.hide()
        title = ""

        setContentView(b.root)

        // UI ONLY: popup sizing (x1.25 width, x2 height, capped)
        val dm = resources.displayMetrics
        val targetW = (dm.widthPixels * 0.92f * 1.25f).toInt()
        val targetH = (dm.heightPixels * 0.60f * 2.0f).toInt()
        val w = minOf(targetW, (dm.widthPixels * 0.98f).toInt())
        val h = minOf(targetH, (dm.heightPixels * 0.90f).toInt())
        window.setLayout(w, h)

        b.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        b.recycler.adapter = adapter

        b.close.setOnClickListener { finish() }


        b.input.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                b.send.isEnabled = !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.send.isEnabled = false


        b.send.setOnClickListener {
            val text = b.input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            b.input.setText("")
            onUserSend(text)
        }

        FirebaseRealtime.ready(
            onReady = {
                Chato.refreshRemoteConfig { c ->
                    runOnUiThread {
                        cfg = c
                        val primary = Chato.resolvePrimaryColor(this)
                        adapter.setCustomerBubbleColor(primary)
                        applySendButtonTheme(primary)
                        startFlow()
                    }
                }
            },
            onError = { e ->
                android.util.Log.e("CHATO", "Firebase auth failed: ${e.message}", e)
            }
        )
    }

    private fun startFlow() {
        val existing = Chato.getExistingSessionIdOrNull()
        if (!existing.isNullOrBlank()) {
            step = PrechatStep.DONE
            ensureSessionAndStartRealtime(clearUiFirst = true)
            return
        }
        startPrechat()
    }

    private fun startPrechat() {
        val pre = cfg?.prechat
        val q1 = pre?.q1.orEmpty().trim()
        val q2 = pre?.q2.orEmpty().trim()
        val q3 = pre?.q3.orEmpty().trim()

        if (q1.isBlank() && q2.isBlank() && q3.isBlank()) {
            step = PrechatStep.DONE
            Chato.getOrCreateSessionId()
            ensureSessionAndStartRealtime(clearUiFirst = true)
            return
        }

        adapter.submitList(emptyList())
        buffered.clear()
        seenMessageIds.clear()

        when {
            q1.isNotBlank() -> {
                step = PrechatStep.WAIT_A1
                botSayAndBuffer(q1)
            }
            q2.isNotBlank() -> {
                step = PrechatStep.WAIT_A2
                botSayAndBuffer(q2)
            }
            else -> finishWithQ3()
        }
    }

    private fun applySendButtonTheme(primary: Int) {
        // disabled background (light gray)
        val disabledBg = Color.parseColor("#E6E6E6")

        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled)
        )
        val colors = intArrayOf(primary, disabledBg)

        // Tint the button background based on enabled/disabled state
        b.send.backgroundTintList = ColorStateList(states, colors)

        // Keep the airplane icon readable (you can switch to white if you want)
        b.send.imageTintList = ColorStateList.valueOf(Color.BLACK)
    }


    private fun onUserSend(text: String) {
        if (step == PrechatStep.DONE) {
            sendToBackend(text)
            return
        }

        addLocalCustomerMessage(text)
        buffered.add("customer" to text)

        val q2 = cfg?.prechat?.q2.orEmpty().trim()

        when (step) {
            PrechatStep.WAIT_A1 -> {
                if (q2.isNotBlank()) {
                    step = PrechatStep.WAIT_A2
                    botSayAndBuffer(q2)
                } else {
                    finishWithQ3()
                }
            }
            PrechatStep.WAIT_A2 -> finishWithQ3()
            else -> {}
        }
    }

    private fun finishWithQ3() {
        val q3 = cfg?.prechat?.q3.orEmpty().trim()
        if (q3.isNotBlank()) botSayAndBuffer(q3)

        step = PrechatStep.DONE
        Chato.getOrCreateSessionId()
        flushBufferedToBackendThenStartRealtime()
    }

    private fun botSayAndBuffer(text: String) {
        if (text.isBlank()) return
        adapter.submitAppend(ChatoMessage(at = nowMs(), from = "bot", text = text))
        buffered.add("bot" to text)
        b.recycler.scrollToPosition(adapter.itemCount - 1)
    }

    private fun addLocalCustomerMessage(text: String) {
        adapter.submitAppend(ChatoMessage(at = nowMs(), from = "customer", text = text))
        b.recycler.scrollToPosition(adapter.itemCount - 1)
    }

    private fun flushBufferedToBackendThenStartRealtime() {
        val apiKey = Chato.getApiKey()
        val api = Chato.getApi()
        val sessionId = Chato.getOrCreateSessionId()

        if (buffered.isEmpty()) {
            ensureSessionAndStartRealtime(clearUiFirst = true)
            return
        }

        lifecycleScope.launch {
            for ((from, text) in buffered) {
                val mappedFrom = if (from == "bot") "owner" else "customer"
                try {
                    api.sendMessage(
                        apiKey = apiKey,
                        body = SendMessageReq(text = text, sessionId = sessionId, from = mappedFrom)
                    )
                } catch (_: Exception) {
                    break
                }
            }
            runOnUiThread { ensureSessionAndStartRealtime(clearUiFirst = true) }
        }
    }

    private fun ensureSessionAndStartRealtime(clearUiFirst: Boolean) {
        if (clearUiFirst) {
            adapter.submitList(emptyList())
            seenMessageIds.clear()
        }
        startRealtime()
    }

    private fun startRealtime() {
        val apiKey = Chato.getApiKey()
        val sessionId = Chato.getExistingSessionIdOrNull() ?: return

        val q = FirebaseRealtime.db()
            .reference
            .child("messages")
            .child(apiKey)
            .child(sessionId)
            .limitToLast(200)

        childListener?.let { msgsQuery?.removeEventListener(it) }

        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, p: String?) {
                val id = snapshot.key ?: return
                if (!seenMessageIds.add(id)) return

                val text = snapshot.child("text").getValue(String::class.java) ?: return
                val from = snapshot.child("from").getValue(String::class.java) ?: "customer"
                val at = snapshot.child("at").getValue(Long::class.java) ?: 0L

                adapter.submitAppend(ChatoMessage(at = at, from = from, text = text))
                b.recycler.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(s: com.google.firebase.database.DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(s: com.google.firebase.database.DataSnapshot, p: String?) {}
            override fun onCancelled(e: com.google.firebase.database.DatabaseError) {}
        }

        q.addChildEventListener(listener)
        msgsQuery = q
        childListener = listener
    }

    private fun sendToBackend(text: String) {
        val apiKey = Chato.getApiKey()
        val api = Chato.getApi()
        val sessionId = Chato.getOrCreateSessionId()

        lifecycleScope.launch {
            try {
                api.sendMessage(
                    apiKey = apiKey,
                    body = SendMessageReq(text = text, sessionId = sessionId, from = "customer")
                )
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        childListener?.let { msgsQuery?.removeEventListener(it) }
        childListener = null
        msgsQuery = null
    }
}
