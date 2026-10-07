package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val isUser: Boolean,
    val messageText: String,
    val language: String = "en", // "en" or "te"
    val actionType: String? = null, // e.g. "OPEN_APP", "TIMER", "ALARM", "FLASHLIGHT", "SYSTEM_INFO", "GEMINI_AI"
    val actionDetail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
