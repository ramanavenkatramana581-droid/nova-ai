package com.example.intent

sealed class AssistantAction {
    data class OpenApp(
        val appName: String,
        val targetPackage: String? = null,
        val webFallbackUrl: String? = null
    ) : AssistantAction()

    data class SetTimer(
        val seconds: Int,
        val label: String
    ) : AssistantAction()

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val message: String
    ) : AssistantAction()

    object TakeCameraPhoto : AssistantAction()

    data class ToggleFlashlight(val enable: Boolean) : AssistantAction()

    object QueryBattery : AssistantAction()

    object QueryDateTime : AssistantAction()

    data class WebSearch(val query: String) : AssistantAction()

    data class GeneralAiQuery(val query: String) : AssistantAction()

    object StopSpeaking : AssistantAction()
}

data class ParsedCommandResult(
    val action: AssistantAction,
    val spokenResponse: String,
    val isTelugu: Boolean = false,
    val actionDetail: String? = null
)
