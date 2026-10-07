package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeechStarted?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeechFinished?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeechFinished?.invoke()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onSpeechFinished?.invoke()
                    }
                })
                onInitComplete(true)
            } else {
                isInitialized = false
                onInitComplete(false)
            }
        }
    }

    fun speak(
        text: String,
        isTelugu: Boolean,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f
    ) {
        if (!isInitialized || text.isBlank()) {
            onSpeechFinished?.invoke()
            return
        }

        val engine = tts ?: return

        // Set Language
        val locale = if (isTelugu) {
            Locale.forLanguageTag("te-IN")
        } else {
            Locale.US
        }

        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to default if specific locale unavailable
            engine.language = Locale.getDefault()
        }

        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        engine.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

        val utteranceId = UUID.randomUUID().toString()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        onSpeechFinished?.invoke()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
