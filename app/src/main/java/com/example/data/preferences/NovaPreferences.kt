package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

enum class NovaThemeAccent(val id: String, val displayName: String, val hexColor: Long) {
    CYAN("CYAN", "Arc Cyan", 0xFF00E5FF),
    EMERALD("EMERALD", "Matrix Emerald", 0xFF00FF88),
    AMBER("AMBER", "Cyber Amber", 0xFFFF9100),
    VIOLET("VIOLET", "Void Violet", 0xFFB388FF),
    CRIMSON("CRIMSON", "Crimson Protocol", 0xFFFF1744)
}

class NovaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_assistant_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ASSISTANT_NAME = "key_assistant_name"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_VOICE_ENABLED = "key_voice_enabled"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_THEME_ACCENT = "key_theme_accent"
        private const val KEY_CUSTOM_API_KEY = "key_custom_api_key"
        private const val KEY_USE_GEMINI = "key_use_gemini"
    }

    var assistantName: String
        get() = prefs.getString(KEY_ASSISTANT_NAME, "NOVA AI") ?: "NOVA AI"
        set(value) = prefs.edit().putString(KEY_ASSISTANT_NAME, value.trim().ifEmpty { "NOVA AI" }).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var themeAccent: NovaThemeAccent
        get() {
            val saved = prefs.getString(KEY_THEME_ACCENT, NovaThemeAccent.CYAN.id)
            return NovaThemeAccent.entries.firstOrNull { it.id == saved } ?: NovaThemeAccent.CYAN
        }
        set(value) = prefs.edit().putString(KEY_THEME_ACCENT, value.id).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var useGemini: Boolean
        get() = prefs.getBoolean(KEY_USE_GEMINI, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_GEMINI, value).apply()
}
