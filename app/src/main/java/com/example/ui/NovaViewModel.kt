package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiService
import com.example.data.ai.SciFiPersonaEngine
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationRepository
import com.example.data.local.NovaDatabase
import com.example.data.preferences.NovaPreferences
import com.example.data.preferences.NovaThemeAccent
import com.example.intent.AssistantAction
import com.example.intent.CommandIntentParser
import com.example.intent.ExecutionResult
import com.example.intent.ParsedCommandResult
import com.example.intent.SystemActionExecutor
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceInputState
import com.example.voice.VoiceRecognitionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = NovaPreferences(application)
    private val database = NovaDatabase.getInstance(application)
    private val repository = ConversationRepository(database.conversationDao())
    private val geminiService = GeminiService()
    private val actionExecutor = SystemActionExecutor(application)
    val voiceManager = VoiceRecognitionManager(application)

    private val ttsManager = TextToSpeechManager(application) { ready ->
        _uiState.update { it.copy(isTtsReady = ready) }
    }

    private val _uiState = MutableStateFlow(NovaUiState())
    val uiState: StateFlow<NovaUiState> = _uiState.asStateFlow()

    private var messageCollectionJob: Job? = null

    init {
        // Initialize state from preferences
        val initialSession = generateNewSessionId()
        _uiState.update {
            it.copy(
                assistantName = preferences.assistantName,
                language = preferences.language,
                themeAccent = preferences.themeAccent,
                isVoiceEnabled = preferences.isVoiceEnabled,
                speechRate = preferences.speechRate,
                speechPitch = preferences.speechPitch,
                customApiKey = preferences.customApiKey,
                activeSessionId = initialSession
            )
        }

        // Setup TTS listener
        ttsManager.onSpeechStarted = {
            _uiState.update { it.copy(status = AssistantStatus.SPEAKING) }
        }
        ttsManager.onSpeechFinished = {
            if (_uiState.value.status == AssistantStatus.SPEAKING) {
                _uiState.update { it.copy(status = AssistantStatus.IDLE) }
            }
        }

        // Observe Voice recognizer
        viewModelScope.launch {
            voiceManager.voiceState.collectLatest { state ->
                when (state) {
                    is VoiceInputState.Idle -> {
                        if (_uiState.value.status == AssistantStatus.LISTENING) {
                            _uiState.update { it.copy(status = AssistantStatus.IDLE, rmsVolume = 0f) }
                        }
                    }
                    is VoiceInputState.Listening -> {
                        _uiState.update {
                            it.copy(
                                status = AssistantStatus.LISTENING,
                                rmsVolume = state.normalizedRms
                            )
                        }
                    }
                    is VoiceInputState.Processing -> {
                        _uiState.update {
                            it.copy(
                                status = AssistantStatus.PROCESSING,
                                rmsVolume = 0f
                            )
                        }
                    }
                    is VoiceInputState.Success -> {
                        _uiState.update {
                            it.copy(
                                status = AssistantStatus.PROCESSING,
                                currentInput = "",
                                rmsVolume = 0f
                            )
                        }
                        processCommand(state.recognizedText)
                        voiceManager.resetState()
                    }
                    is VoiceInputState.Error -> {
                        _uiState.update {
                            it.copy(
                                status = AssistantStatus.IDLE,
                                lastSystemNotice = state.message,
                                rmsVolume = 0f
                            )
                        }
                        voiceManager.resetState()
                    }
                }
            }
        }

        // Observe Messages for current session
        observeSessionMessages(initialSession)

        // Observe distinct sessions for history
        viewModelScope.launch {
            repository.getSessions().collectLatest { sessions ->
                _uiState.update { it.copy(distinctSessions = sessions) }
            }
        }

        // Initial welcome message if conversation is empty
        viewModelScope.launch {
            delay(400)
            val count = repository.getCount()
            if (count == 0) {
                val greeting = if (preferences.language == "te") {
                    "${preferences.assistantName} ఆన్‌లైన్‌లో ఉంది. సిస్టమ్స్ సిద్ధంగా ఉన్నాయి. మీకు ఎలా సహాయపడాలి?"
                } else {
                    "${preferences.assistantName} systems operational. Acoustic sensors and neural core standing by. How may I assist you?"
                }
                saveAssistantMessage(greeting, "SYSTEM_INIT", null)
            }
        }
    }

    private fun generateNewSessionId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "Session_${sdf.format(Date())}"
    }

    private fun observeSessionMessages(sessionId: String) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collectLatest { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun onInputChange(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun onSendTextCommand() {
        val text = _uiState.value.currentInput.trim()
        if (text.isBlank()) return
        _uiState.update { it.copy(currentInput = "") }
        processCommand(text)
    }

    fun onQuickCommandSelected(command: String) {
        processCommand(command)
    }

    fun toggleVoiceListening() {
        if (_uiState.value.status == AssistantStatus.SPEAKING) {
            stopSpeaking()
            return
        }

        if (_uiState.value.status == AssistantStatus.LISTENING) {
            voiceManager.stopListening()
            _uiState.update { it.copy(status = AssistantStatus.IDLE, rmsVolume = 0f) }
        } else {
            ttsManager.stop()
            voiceManager.startListening(_uiState.value.language)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _uiState.update { it.copy(status = AssistantStatus.IDLE) }
    }

    fun replayMessageSpeech(text: String, language: String) {
        if (!_uiState.value.isVoiceEnabled) return
        val isTe = language == "te" || _uiState.value.language == "te"
        ttsManager.speak(
            text = text,
            isTelugu = isTe,
            speechRate = _uiState.value.speechRate,
            speechPitch = _uiState.value.speechPitch
        )
    }

    fun processCommand(commandText: String) {
        if (commandText.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(status = AssistantStatus.PROCESSING) }

            // Save user message
            val lang = _uiState.value.language
            repository.addMessage(
                ConversationEntity(
                    sessionId = _uiState.value.activeSessionId,
                    isUser = true,
                    messageText = commandText,
                    language = lang
                )
            )

            // Parse intent
            val parser = CommandIntentParser(_uiState.value.assistantName)
            val parsed = parser.parse(commandText, lang)

            when (parsed.action) {
                is AssistantAction.StopSpeaking -> {
                    stopSpeaking()
                    respondWith(parsed.spokenResponse, "STOP", null, parsed.isTelugu)
                }

                is AssistantAction.GeneralAiQuery -> {
                    handleAiQuery(parsed.action.query, parsed.isTelugu)
                }

                is AssistantAction.QueryBattery -> {
                    val result = actionExecutor.getBatteryTelemetry()
                    val reply = if (parsed.isTelugu) {
                        if (result is ExecutionResult.Telemetry) {
                            "బ్యాటరీ స్థాయి: ${result.level}%. " + if (result.isCharging) "ఛార్జింగ్ అవుతోంది." else "నార్మల్ డిశ్చార్జ్."
                        } else {
                            "బ్యాటరీ వివరాలు పొందబడ్డాయి."
                        }
                    } else {
                        if (result is ExecutionResult.Telemetry) result.message else "Power cell telemetry retrieved."
                    }
                    respondWith(reply, "SYSTEM_INFO", "Battery Diagnostic", parsed.isTelugu)
                }

                is AssistantAction.QueryDateTime -> {
                    val result = actionExecutor.getDateTimeTelemetry()
                    val reply = if (parsed.isTelugu) {
                        val sdf = SimpleDateFormat("h:mm a, EEEE, d MMMM", Locale.forLanguageTag("te-IN"))
                        "ప్రస్తుత సమయం: ${sdf.format(Date())}."
                    } else {
                        if (result is ExecutionResult.Telemetry) result.message else "Chrono sync completed."
                    }
                    respondWith(reply, "SYSTEM_INFO", "Chronometer", parsed.isTelugu)
                }

                else -> {
                    // System execution (App launch, timer, alarm, camera, flashlight, web search)
                    val execResult = actionExecutor.executeAction(parsed.action)
                    _uiState.update { it.copy(status = AssistantStatus.ACTION_EXECUTED) }

                    val actionNotice = when (execResult) {
                        is ExecutionResult.Success -> execResult.message
                        is ExecutionResult.Notice -> execResult.message
                        is ExecutionResult.Telemetry -> execResult.message
                    }

                    respondWith(
                        text = parsed.spokenResponse,
                        actionType = parsed.action::class.simpleName ?: "ACTION",
                        actionDetail = actionNotice,
                        isTelugu = parsed.isTelugu
                    )
                }
            }
        }
    }

    private suspend fun handleAiQuery(query: String, isTelugu: Boolean) {
        val currentName = _uiState.value.assistantName
        val customKey = _uiState.value.customApiKey

        // Try Gemini API first
        val geminiResult = geminiService.generateAssistantResponse(
            prompt = query,
            assistantName = currentName,
            language = if (isTelugu) "te" else "en",
            customApiKey = customKey
        )

        val reply = geminiResult.getOrElse {
            // Resilient sci-fi neural persona engine fallback!
            SciFiPersonaEngine.generateLocalSciFiResponse(query, currentName, isTelugu)
        }

        respondWith(
            text = reply,
            actionType = "AI_TRANSMISSION",
            actionDetail = if (geminiResult.isSuccess) "Gemini Neural Core" else "Local Sci-Fi Persona",
            isTelugu = isTelugu
        )
    }

    private suspend fun respondWith(
        text: String,
        actionType: String,
        actionDetail: String?,
        isTelugu: Boolean
    ) {
        saveAssistantMessage(text, actionType, actionDetail)

        if (_uiState.value.isVoiceEnabled) {
            ttsManager.speak(
                text = text,
                isTelugu = isTelugu,
                speechRate = _uiState.value.speechRate,
                speechPitch = _uiState.value.speechPitch
            )
        } else {
            delay(1000)
            _uiState.update { it.copy(status = AssistantStatus.IDLE) }
        }
    }

    private suspend fun saveAssistantMessage(
        text: String,
        actionType: String?,
        actionDetail: String?
    ) {
        repository.addMessage(
            ConversationEntity(
                sessionId = _uiState.value.activeSessionId,
                isUser = false,
                messageText = text,
                language = _uiState.value.language,
                actionType = actionType,
                actionDetail = actionDetail
            )
        )
    }

    // Settings & Dialogs
    fun openSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun openHistory(open: Boolean) {
        _uiState.update { it.copy(isHistoryOpen = open) }
    }

    fun openPermissionRationale(open: Boolean) {
        _uiState.update { it.copy(isPermissionRationaleOpen = open) }
    }

    fun updateAssistantName(name: String) {
        val clean = name.trim().ifEmpty { "NOVA AI" }
        preferences.assistantName = clean
        _uiState.update { it.copy(assistantName = clean) }
    }

    fun updateLanguage(langCode: String) {
        preferences.language = langCode
        _uiState.update { it.copy(language = langCode) }
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        preferences.isVoiceEnabled = enabled
        if (!enabled) ttsManager.stop()
        _uiState.update { it.copy(isVoiceEnabled = enabled) }
    }

    fun updateSpeechRate(rate: Float) {
        preferences.speechRate = rate
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun updateSpeechPitch(pitch: Float) {
        preferences.speechPitch = pitch
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun updateThemeAccent(accent: NovaThemeAccent) {
        preferences.themeAccent = accent
        _uiState.update { it.copy(themeAccent = accent) }
    }

    fun updateCustomApiKey(key: String) {
        preferences.customApiKey = key
        _uiState.update { it.copy(customApiKey = key) }
    }

    // Session Management
    fun startNewSession() {
        val newSessionId = generateNewSessionId()
        _uiState.update { it.copy(activeSessionId = newSessionId) }
        observeSessionMessages(newSessionId)

        viewModelScope.launch {
            val greeting = if (_uiState.value.language == "te") {
                "క్రొత్త సెషన్ ప్రారంభించబడింది. ${_uiState.value.assistantName} ఆదేశాల కోసం వేచి ఉంది."
            } else {
                "New mission session initialized. ${_uiState.value.assistantName} standing by."
            }
            saveAssistantMessage(greeting, "NEW_SESSION", null)
        }
    }

    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId, isHistoryOpen = false) }
        observeSessionMessages(sessionId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_uiState.value.activeSessionId == sessionId) {
                startNewSession()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            startNewSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.stopListening()
        ttsManager.shutdown()
    }
}
