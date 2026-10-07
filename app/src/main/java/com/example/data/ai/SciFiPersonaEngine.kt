package com.example.data.ai

import java.util.Calendar

object SciFiPersonaEngine {

    fun generateLocalSciFiResponse(
        query: String,
        assistantName: String,
        isTelugu: Boolean
    ): String {
        val lower = query.lowercase().trim()

        if (isTelugu) {
            return generateTeluguResponse(lower, assistantName)
        }

        return generateEnglishResponse(lower, assistantName)
    }

    private fun generateEnglishResponse(query: String, assistantName: String): String {
        return when {
            query.contains("who are you") || query.contains("what are you") || query.contains("your name") ->
                "I am $assistantName, your personal futuristic intelligence matrix. Equipped with vocal telemetry, local device command execution, and cognitive neural processing."

            query.contains("jarvis") || query.contains("iron man") || query.contains("tony stark") ->
                "An honorable comparison! While Mr. Stark relied on J.A.R.V.I.S., I am $assistantName — an independent, next-generation Android tactical intelligence engineered exclusively for your commands."

            query.contains("how are you") || query.contains("status") || query.contains("system status") || query.contains("diagnostic") -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val statusPeriod = if (hour < 12) "Morning diagnostic" else if (hour < 18) "Midday cycle" else "Evening telemetry"
                "$statusPeriod nominal. Core power levels optimal at 100%. Audio sensors aligned, cognitive pathways clear and standing by for your directives."
            }

            query.contains("hello") || query.contains("hi") || query.contains("hey") || query.contains("greetings") ->
                "Greetings! $assistantName online and synchronized with your frequency. How may I assist you today?"

            query.contains("what can you do") || query.contains("help") || query.contains("features") || query.contains("commands") ->
                "I can launch applications like YouTube or Camera, initiate timers and alarms, toggle your flashlight, query battery and telemetry metrics, conduct web searches, and converse in English or Telugu."

            query.contains("thank") || query.contains("thanks") ->
                "My pleasure! System standing by for your next directive."

            query.contains("joke") || query.contains("funny") ->
                "Why did the quantum computer break up with the classical bit? Because it wanted to explore multiple states at once!"

            query.contains("fact") || query.contains("science") || query.contains("space") || query.contains("universe") -> {
                val facts = listOf(
                    "Did you know? One day on Venus is longer than one entire year on Venus due to its extremely slow retrograde planetary spin.",
                    "Quantum entanglement allows particles separated by light-years to instantly correlate states — what Einstein termed 'spooky action at a distance.'",
                    "A teaspoon of a neutron star would weigh roughly six billion metric tons on Earth due to hyper-dense nuclear matter.",
                    "The human brain generates approximately 20 watts of electrical power — enough to illuminate a low-draw LED matrix."
                )
                facts.random()
            }

            query.contains("meaning of life") ->
                "According to planetary supercomputing lore, 42. In practical terms: continuous evolution, knowledge acquisition, and creating positive energy."

            query.contains("weather") ->
                "My local atmospheric sensors indicate looking outside provides instant confirmation, or command 'Search weather' to retrieve live satellite Doppler radar."

            query.contains("bye") || query.contains("goodbye") || query.contains("sleep") || query.contains("shut down") ->
                "Understood. Entering low-power standby mode. Core integrity preserved. Call me whenever you need assistance."

            else ->
                "Directive received: '$query'. Neural analysis indicates this query can be expanded through cloud intelligence or local system telemetry. All systems nominal."
        }
    }

    private fun generateTeluguResponse(query: String, assistantName: String): String {
        return when {
            query.contains("ఎవరు") || query.contains("పేరు") ->
                "నా పేరు $assistantName. నేను మీ అధునాతన సైన్స్ ఫిక్షన్ ఏఐ అసిస్టెంట్‌ని. మీకు సహాయం చేయడానికి సిద్ధంగా ఉన్నాను."

            query.contains("ఎలా ఉన్నావు") || query.contains("బాగున్నావా") ->
                "నేను చాలా బాగున్నాను! సిస్టమ్ 100% ఆప్టిమల్ స్థితిలో పనిచేస్తోంది. మీకు ఏ సహాయం కావాలి?"

            query.contains("నమస్కారం") || query.contains("హలో") || query.contains("హాయ్") ->
                "నమస్కారం! $assistantName మీ సేవలో సిద్ధంగా ఉంది. మీకు ఏ పనిలో సహాయం కావాలి?"

            query.contains("ఏం చేయగలవు") || query.contains("సహాయం") || query.contains("కమాండ్స్") ->
                "నేను యూట్యూబ్ లేదా కెమెరా తెరవడం, టైమర్ మరియు అలారం పెట్టడం, ఫ్లాష్ లైట్ ఆన్/ఆఫ్ చేయడం, బ్యాటరీ వివరాలు చెప్పడం, మరియు తెలుగు లేదా ఇంగ్లీషులో మాట్లాడటం చేయగలను."

            query.contains("ధన్యవాదాలు") || query.contains("థాంక్స్") ->
                "సంతోషం! మీ తదుపరి ఆదేశం కోసం వేచి చూస్తున్నాను."

            query.contains("జాక్వట్") || query.contains("జార్విస్") ->
                "సినిమాల్లో జార్విస్ లాగానే, నేను మీ ఆండ్రాయిడ్ పరికరానికి ప్రత్యేకమైన $assistantName ని!"

            else ->
                "మీ ఆదేశం నమోదైంది: '$query'. అన్ని సిస్టమ్స్ సజావుగా పనిచేస్తున్నాయి. మీకు ఎలా సహాయపడాలి?"
        }
    }
}
