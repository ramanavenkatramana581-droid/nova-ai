package com.example.intent

import java.util.Calendar
import java.util.regex.Pattern

class CommandIntentParser(private val assistantName: String) {

    fun parse(rawInput: String, preferredLanguage: String = "auto"): ParsedCommandResult {
        val trimmed = rawInput.trim()
        val lower = trimmed.lowercase()

        val isTelugu = when (preferredLanguage) {
            "te" -> true
            "en" -> false
            else -> containsTelugu(trimmed)
        }

        // 1. Stop / Silence command
        if (matchesStop(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.StopSpeaking,
                spokenResponse = if (isTelugu) "ఆగాను. ఆదేశాలు వేచి చూస్తున్నాయి." else "Standing down. Audio output silenced.",
                isTelugu = isTelugu
            )
        }

        // 2. Flashlight Toggle
        if (matchesFlashlightOn(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.ToggleFlashlight(true),
                spokenResponse = if (isTelugu) "ఫ్లాష్ లైట్ ఆన్ చేయబడింది." else "High-lumens tactical emitter activated.",
                isTelugu = isTelugu,
                actionDetail = "Flashlight: ON"
            )
        }
        if (matchesFlashlightOff(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.ToggleFlashlight(false),
                spokenResponse = if (isTelugu) "ఫ్లాష్ లైట్ ఆఫ్ చేయబడింది." else "Illumination grid deactivated.",
                isTelugu = isTelugu,
                actionDetail = "Flashlight: OFF"
            )
        }

        // 3. Camera / Take Photo
        if (matchesCamera(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.TakeCameraPhoto,
                spokenResponse = if (isTelugu) "కెమెరా తెరవబడుతోంది. ఫోటో తీయండి." else "Optical sensors engaged. Launching camera matrix.",
                isTelugu = isTelugu,
                actionDetail = "Camera Matrix Activated"
            )
        }

        // 4. Timer Setting
        val timerSeconds = extractTimerSeconds(lower)
        if (timerSeconds != null) {
            val minutes = timerSeconds / 60
            val secondsRem = timerSeconds % 60
            val durationLabel = when {
                minutes > 0 && secondsRem > 0 -> "$minutes min $secondsRem sec"
                minutes > 0 -> "$minutes minute" + if (minutes > 1) "s" else ""
                else -> "$secondsRem seconds"
            }
            return ParsedCommandResult(
                action = AssistantAction.SetTimer(timerSeconds, "NOVA Timer: $durationLabel"),
                spokenResponse = if (isTelugu)
                    "$durationLabel కొరకు టైమర్ సిద్ధం చేయబడింది."
                else
                    "Temporal countdown sequence initiated for $durationLabel.",
                isTelugu = isTelugu,
                actionDetail = "Timer: $durationLabel"
            )
        }

        // 5. Alarm Setting
        val alarmTime = extractAlarmTime(lower)
        if (alarmTime != null) {
            val (hour, minute) = alarmTime
            val formattedTime = String.format("%02d:%02d", hour, minute)
            return ParsedCommandResult(
                action = AssistantAction.SetAlarm(hour, minute, "NOVA Wakeup Alarm"),
                spokenResponse = if (isTelugu)
                    "$formattedTime వద్ద అలారం అమర్చబడింది."
                else
                    "Chrono alarm synchronized for $formattedTime.",
                isTelugu = isTelugu,
                actionDetail = "Alarm: $formattedTime"
            )
        }

        // 6. Battery Query
        if (matchesBattery(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.QueryBattery,
                spokenResponse = if (isTelugu) "పరికర బ్యాటరీ టెలిమెట్రీని తనిఖీ చేస్తున్నాను..." else "Checking power cell voltage levels...",
                isTelugu = isTelugu,
                actionDetail = "Power Cell Diagnostic"
            )
        }

        // 7. Time / Date Query
        if (matchesDateTime(lower)) {
            return ParsedCommandResult(
                action = AssistantAction.QueryDateTime,
                spokenResponse = if (isTelugu) "సిస్టమ్ సమయం ధ్రువీకరించబడుతోంది..." else "Accessing atomic chrono telemetry...",
                isTelugu = isTelugu,
                actionDetail = "Temporal Synchronization"
            )
        }

        // 8. Open Specific Apps
        val appAction = detectOpenApp(lower, isTelugu)
        if (appAction != null) {
            return appAction
        }

        // 9. Web Search
        val searchQuery = extractSearchQuery(lower)
        if (!searchQuery.isNullOrBlank()) {
            return ParsedCommandResult(
                action = AssistantAction.WebSearch(searchQuery),
                spokenResponse = if (isTelugu)
                    "'$searchQuery' కోసం వెబ్‌లో శోధిస్తున్నాను."
                else
                    "Accessing planetary data network. Searching for '$searchQuery'.",
                isTelugu = isTelugu,
                actionDetail = "Query: $searchQuery"
            )
        }

        // 10. General conversational AI Query
        return ParsedCommandResult(
            action = AssistantAction.GeneralAiQuery(trimmed),
            spokenResponse = "", // To be resolved via Gemini / SciFiPersona
            isTelugu = isTelugu
        )
    }

    private fun containsTelugu(text: String): Boolean {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.TELUGU) return true
        }
        return false
    }

    private fun matchesStop(input: String): Boolean {
        return input == "stop" || input == "shut up" || input == "be quiet" ||
                input == "silence" || input == "cancel" || input == "ఆగు" ||
                input == "చాలు" || input == "ఆపు"
    }

    private fun matchesFlashlightOn(input: String): Boolean {
        return (input.contains("flash") || input.contains("torch") || input.contains("ఫ్లాష్") || input.contains("టార్చ్")) &&
                (input.contains("on") || input.contains("start") || input.contains("enable") ||
                        input.contains("ఆన్") || input.contains("వెలిగించు"))
    }

    private fun matchesFlashlightOff(input: String): Boolean {
        return (input.contains("flash") || input.contains("torch") || input.contains("ఫ్లాష్") || input.contains("టార్చ్")) &&
                (input.contains("off") || input.contains("stop") || input.contains("disable") ||
                        input.contains("ఆఫ్") || input.contains("ఆపు"))
    }

    private fun matchesCamera(input: String): Boolean {
        return input.contains("camera") || input.contains("photo") || input.contains("picture") ||
                input.contains("selfie") || input.contains("కెమెరా") || input.contains("ఫోటో") ||
                input.contains("తీయి") || input.contains("తియ్యి") || input.contains("camra")
    }

    private fun matchesBattery(input: String): Boolean {
        return input.contains("battery") || input.contains("power level") ||
                input.contains("charge") || input.contains("బ్యాటరీ") ||
                input.contains("ఛార్జింగ్")
    }

    private fun matchesDateTime(input: String): Boolean {
        return input.contains("time") || input.contains("date") || input.contains("clock") ||
                input.contains("సమయం") || input.contains("టైమ్") || input.contains("తేదీ")
    }

    private fun detectOpenApp(input: String, isTelugu: Boolean): ParsedCommandResult? {
        val launchWords = listOf("open", "launch", "start", "run", "go to", "play", "ఓపెన్", "తెరువు", "స్టార్ట్")
        val isLaunchIntent = launchWords.any { input.contains(it) } || input.startsWith("open ")

        // YouTube
        if (input.contains("youtube") || input.contains("యూట్యూబ్") || input.contains("youtub")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("YouTube", "com.google.android.youtube", "https://www.youtube.com"),
                spokenResponse = if (isTelugu) "యూట్యూబ్ తెరవబడుతోంది." else "Accessing audiovisual streaming grid: YouTube.",
                isTelugu = isTelugu,
                actionDetail = "Launch YouTube"
            )
        }

        // Browser / Chrome
        if (input.contains("chrome") || input.contains("browser") || input.contains("గూగుల్ క్రోమ్") || input.contains("క్రోమ్") || input.contains("బ్రెజర్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Browser", "com.android.chrome", "https://www.google.com"),
                spokenResponse = if (isTelugu) "బ్రౌజర్ తెరవబడుతోంది." else "Launching global cyberspace browser.",
                isTelugu = isTelugu,
                actionDetail = "Launch Chrome Browser"
            )
        }

        // Google Maps / Navigation
        if (input.contains("map") || input.contains("maps") || input.contains("navigation") || input.contains("మ్యాప్స్") || input.contains("మ్యాప్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Maps", "com.google.android.apps.maps", "https://maps.google.com"),
                spokenResponse = if (isTelugu) "మ్యాప్స్ తెరవబడుతోంది." else "Initializing planetary GPS navigation satellite link.",
                isTelugu = isTelugu,
                actionDetail = "Launch Google Maps"
            )
        }

        // Calculator
        if (input.contains("calculator") || input.contains("calc") || input.contains("క్యాలిక్యులేటర్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Calculator", "com.google.android.calculator", null),
                spokenResponse = if (isTelugu) "క్యాలిక్యులేటర్ తెరవబడుతోంది." else "Loading quantum computational calculator module.",
                isTelugu = isTelugu,
                actionDetail = "Launch Calculator"
            )
        }

        // WhatsApp
        if (input.contains("whatsapp") || input.contains("వాట్సాప్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("WhatsApp", "com.whatsapp", null),
                spokenResponse = if (isTelugu) "వాట్సాప్ తెరవబడుతోంది." else "Establishing secure communication uplink: WhatsApp.",
                isTelugu = isTelugu,
                actionDetail = "Launch WhatsApp"
            )
        }

        // Settings
        if (input.contains("setting") || input.contains("settings") || input.contains("సెట్టింగ్స్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Settings", null, null),
                spokenResponse = if (isTelugu) "సిస్టమ్ సెట్టింగ్స్ తెరవబడుతున్నాయి." else "Accessing Android operating parameters and settings.",
                isTelugu = isTelugu,
                actionDetail = "Launch System Settings"
            )
        }

        // Clock / Alarms
        if ((input.contains("clock") || input.contains("alarms") || input.contains("గడియారం")) && isLaunchIntent) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Clock", "com.google.android.deskclock", null),
                spokenResponse = if (isTelugu) "గడియారం తెరవబడుతోంది." else "Accessing chrono desk clock subsystem.",
                isTelugu = isTelugu,
                actionDetail = "Launch Clock"
            )
        }

        // Calendar
        if (input.contains("calendar") || input.contains("క్యాలెండర్")) {
            return ParsedCommandResult(
                action = AssistantAction.OpenApp("Calendar", null, null),
                spokenResponse = if (isTelugu) "క్యాలెండర్ తెరవబడుతోంది." else "Retrieving planetary orbital calendar events.",
                isTelugu = isTelugu,
                actionDetail = "Launch Calendar"
            )
        }

        return null
    }

    private fun extractTimerSeconds(input: String): Int? {
        if (!input.contains("timer") && !input.contains("టైమర్") && !input.contains("countdown")) return null

        // Pattern: "timer for 5 minutes", "5 minute timer", "30 seconds", etc.
        val minutePattern = Pattern.compile("(\\d+)\\s*(?:min|minute|minutes|నిమిషాల|నిమిషాలు|నిమిషం)")
        val minMatcher = minutePattern.matcher(input)
        if (minMatcher.find()) {
            val minutes = minMatcher.group(1)?.toIntOrNull() ?: 1
            return minutes * 60
        }

        val secondPattern = Pattern.compile("(\\d+)\\s*(?:sec|second|seconds|సెకన్ల|సెకన్లు)")
        val secMatcher = secondPattern.matcher(input)
        if (secMatcher.find()) {
            return secMatcher.group(1)?.toIntOrNull() ?: 30
        }

        val justNumberPattern = Pattern.compile("(?:timer|టైమర్)\\s*(?:for)?\\s*(\\d+)")
        val numMatcher = justNumberPattern.matcher(input)
        if (numMatcher.find()) {
            val num = numMatcher.group(1)?.toIntOrNull() ?: 5
            return num * 60
        }

        return 60 // Default 1 minute
    }

    private fun extractAlarmTime(input: String): Pair<Int, Int>? {
        if (!input.contains("alarm") && !input.contains("అలారం") && !input.contains("wake me up")) return null

        // 7:30 or 7.30
        val colonPattern = Pattern.compile("(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?")
        val colonMatcher = colonPattern.matcher(input)
        if (colonMatcher.find()) {
            var hour = colonMatcher.group(1)?.toIntOrNull() ?: 7
            val min = colonMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = colonMatcher.group(3)
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return Pair(hour, min)
        }

        // "at 7 am", "for 6 pm", "7 గంటలకు"
        val hourPattern = Pattern.compile("(\\d{1,2})\\s*(am|pm|o'clock|గంటలకు)?")
        val hourMatcher = hourPattern.matcher(input)
        if (hourMatcher.find()) {
            var hour = hourMatcher.group(1)?.toIntOrNull() ?: 7
            val amPm = hourMatcher.group(2)
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return Pair(hour, 0)
        }

        return Pair(7, 0) // Default 7:00 AM
    }

    private fun extractSearchQuery(input: String): String? {
        val searchPrefixes = listOf(
            "search for", "search web for", "google for", "google", "search",
            "గూగుల్ లో వెతుకు", "వెతుకు"
        )
        for (prefix in searchPrefixes) {
            if (input.startsWith(prefix)) {
                return input.removePrefix(prefix).trim().trim(':', ' ', '"', '\'')
            }
        }
        return null
    }
}
