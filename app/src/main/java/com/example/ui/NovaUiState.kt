package com.example.ui

import com.example.data.local.ConversationEntity
import com.example.data.preferences.NovaThemeAccent

enum class AssistantStatus(val label: String) {
    IDLE("SYSTEM STANDBY"),
    LISTENING("ACOUSTIC SCAN"),
    PROCESSING("NEURAL PROCESSING"),
    SPEAKING("VOCAL SYNTHESIS"),
    ACTION_EXECUTED("DIRECTIVE EXECUTED")
}

data class NovaUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val messages: List<ConversationEntity> = emptyList(),
    val currentInput: String = "",
    val assistantName: String = "NOVA AI",
    val language: String = "en", // "en" or "te"
    val themeAccent: NovaThemeAccent = NovaThemeAccent.CYAN,
    val isVoiceEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val customApiKey: String = "",
    val activeSessionId: String = "",
    val rmsVolume: Float = 0f,
    val lastSystemNotice: String? = null,
    val isSettingsOpen: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isPermissionRationaleOpen: Boolean = false,
    val isTtsReady: Boolean = false,
    val distinctSessions: List<String> = emptyList()
)
