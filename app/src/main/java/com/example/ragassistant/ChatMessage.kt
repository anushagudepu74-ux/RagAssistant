package com.example.ragassistant

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val source: String? = null
)