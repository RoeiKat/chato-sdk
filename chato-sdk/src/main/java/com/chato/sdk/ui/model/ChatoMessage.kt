package com.chato.sdk.ui.model

data class ChatoMessage(
    var at: Long = 0L,
    var from: String = "",
    var text: String = ""
)
