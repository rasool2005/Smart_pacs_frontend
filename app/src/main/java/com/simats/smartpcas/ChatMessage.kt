package com.simats.smartpcas

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isChart: Boolean = false,
    val isTyping: Boolean = false,
    val isQueries: Boolean = false,
    val queries: List<String>? = null
)
