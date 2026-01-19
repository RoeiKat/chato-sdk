package com.chato.sdk.net.dto

data class SendMessageReq(
    val text: String,
    val sessionId: String? = null,
    val from: String? = null // "customer" or "owner" (we'll use owner for bot lines)
)
