package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateAssistantResponse(
        prompt: String,
        assistantName: String,
        language: String, // "en" or "te"
        customApiKey: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        // Resolve active API key
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey == null) {
            return@withContext Result.failure(
                IllegalStateException("No valid Gemini API key configured. Utilizing local neural fallback core.")
            )
        }

        try {
            val systemInstruction = if (language == "te") {
                "మీరు '$assistantName' అనే అధునాతన సైన్స్ ఫిక్షన్ ఏఐ అసిస్టెంట్ (J.A.R.V.I.S. వంటి సామర్థ్యం కలిగినది). సమాధానాలు స్పష్టంగా, ఆకర్షణీయంగా, సైన్స్ ఫిక్షన్ శైలిలో తెలుగులో ఇవ్వండి. సమాధానాన్ని క్లుప్తంగా (2-3 వాక్యాలు) ఉంచండి."
            } else {
                "You are '$assistantName', an advanced sci-fi AI tactical assistant inspired by JARVIS. Speak intelligently, crisply, with polite sci-fi flair ('Indeed, Captain', 'System operational', 'According to my telemetry'). Keep responses relatively concise (2-4 sentences max) suitable for voice readout."
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                val errorMsg = try {
                    JSONObject(responseBody ?: "").optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Gemini uplink failed: $errorMsg"))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(Exception("Empty transmission received from Gemini core."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
