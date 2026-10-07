package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceInputState {
    object Idle : VoiceInputState()
    data class Listening(val normalizedRms: Float) : VoiceInputState()
    object Processing : VoiceInputState()
    data class Success(val recognizedText: String) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val voiceState: StateFlow<VoiceInputState> = _voiceState.asStateFlow()

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageCode: String = "en") {
        if (!isRecognitionAvailable) {
            _voiceState.value = VoiceInputState.Error("Vocal recognition matrix unavailable on this system.")
            return
        }

        stopListening()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceInputState.Listening(0f)
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceInputState.Listening(0.2f)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Map RMS dB (-2 to 10 typical) to 0.0 - 1.0
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _voiceState.value = VoiceInputState.Listening(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceInputState.Processing
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio transmission corrupted."
                    SpeechRecognizer.ERROR_CLIENT -> "Sensor client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Acoustic authorization required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Neural uplink latency detected."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No audible speech detected. Core standing by."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Neural recognizer recalibrating."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Acoustic signal timed out."
                    else -> "Audio capture error: $error"
                }
                _voiceState.value = VoiceInputState.Error(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim()
                if (!recognized.isNullOrEmpty()) {
                    _voiceState.value = VoiceInputState.Success(recognized)
                } else {
                    _voiceState.value = VoiceInputState.Error("Acoustic analysis inconclusive.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrEmpty()) {
                    _voiceState.value = VoiceInputState.Listening(0.5f)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            val locale = if (languageCode == "te") "te-IN" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceInputState.Error("Failed to engage acoustic sensors: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun resetState() {
        stopListening()
        _voiceState.value = VoiceInputState.Idle
    }
}
