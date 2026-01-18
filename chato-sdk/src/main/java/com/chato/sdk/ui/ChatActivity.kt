package com.chato.sdk.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chato.sdk.Chato
import com.chato.sdk.databinding.ActivityChatoChatBinding
import com.chato.sdk.net.dto.SendMessageReq
import com.chato.sdk.realtime.FirebaseRealtime
import com.chato.sdk.ui.model.ChatoMessage
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatoChatBinding
    private val adapter = ChatAdapter()

    private var messagesListener: ChildEventListener? = null

    private var msgsQuery: com.google.firebase.database.Query? = null
    private var childListener: com.google.firebase.database.ChildEventListener? = null
    private val seenMessageIds = HashSet<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatoChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val dm = resources.displayMetrics
        val w = (dm.widthPixels * 0.92).toInt()   // 92% width
        val h = (dm.heightPixels * 0.60).toInt()  // 60% height (>= 50%)
        window.setLayout(w, h)


        b.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        b.recycler.adapter = adapter


        FirebaseRealtime.ready(
            onReady = { startRealtime() },
            onError = { e ->
                android.util.Log.e("CHATO", "Firebase auth failed: ${e.message}", e)
                android.widget.Toast.makeText(this, "Firebase auth failed", android.widget.Toast.LENGTH_LONG).show()
            }
        )


        b.send.setOnClickListener {
            val text = b.input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            b.input.setText("")
            send(text)
        }
        b.close.setOnClickListener { finish() }
    }

    private var msgsListener: com.google.firebase.database.ValueEventListener? = null
    private var msgsRef: com.google.firebase.database.Query? = null

    private fun startRealtime() {
        val apiKey = Chato.getApiKey()
        val sessionId = Chato.getSessionId()

        seenMessageIds.clear()
        adapter.submitList(emptyList())

        val q = com.chato.sdk.realtime.FirebaseRealtime.db()
            .reference
            .child("messages")
            .child(apiKey)
            .child(sessionId)
            .limitToLast(200)

        // Clean any previous listener (safety)
        childListener?.let { l -> msgsQuery?.removeEventListener(l) }

        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                if (!seenMessageIds.add(id)) return

                val text = snapshot.child("text").getValue(String::class.java) ?: return
                val from = snapshot.child("from").getValue(String::class.java) ?: "customer"
                val at = snapshot.child("at").getValue(Long::class.java)
                    ?: snapshot.child("at").getValue(Double::class.java)?.toLong()
                    ?: 0L

                adapter.submitAppend(com.chato.sdk.ui.model.ChatoMessage(at = at, from = from, text = text))
                b.recycler.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        q.addChildEventListener(listener)
        msgsQuery = q
        childListener = listener
    }


    override fun onDestroy() {
        super.onDestroy()
        childListener?.let { l -> msgsQuery?.removeEventListener(l) }
        childListener = null
        msgsQuery = null
    }




    private fun send(text: String) {
        val apiKey = Chato.getApiKey()
        val api = Chato.getApi()

        val currentSessionId = Chato.getSessionId()

        b.recycler.scrollToPosition(adapter.itemCount - 1)

        lifecycleScope.launch {
            try {
                val res = api.sendMessage(
                    apiKey = apiKey,
                    body = com.chato.sdk.net.dto.SendMessageReq(text = text, sessionId = currentSessionId)
                )

            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Send failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
