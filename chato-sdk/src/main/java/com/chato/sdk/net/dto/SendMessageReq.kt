package com.chato.sdk.net.dto

data class SendMessageReq(
    val text: String,
    val sessionId: String? // allow null (server creates) but we will pass ours
)
