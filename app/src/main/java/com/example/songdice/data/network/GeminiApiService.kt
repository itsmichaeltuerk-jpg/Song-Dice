package com.example.songdice.data.network

import com.example.songdice.data.model.SongArrangement
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiApiService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val arrangementAdapter = moshi.adapter(SongArrangement::class.java)

    /**
     * Executes a request to Gemini API to generate or adapt a multi-track musical arrangement.
     */
    suspend fun generateArrangement(
        apiKey: String,
        promptText: String
    ): SongArrangement {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("Missing valid Gemini API Key. Please configure your API key in AI Studio Secrets.")
        }

        val url = "$BASE_URL?key=$apiKey"

        // Build Gemini GenerateContent JSON payload
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", promptText)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Force JSON output formatting
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            }
            put("generationConfig", generationConfig)

            // System instruction for strict music theory output
            val systemInstruction = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        put("text", """
                            You are a world-class Music Producer, Master Composer, and Music Theory AI. 
                            Your goal is to generate pristine multi-track musical arrangements (Drums, Bass, Chords, Melody) as structured JSON.
                            Follow standard MIDI mapping:
                            - Drums on Channel 9 (MIDI Ch 10): Kick=36, Snare=38, Hi-Hat Close=42, Hi-Hat Open=46, Crash=49, Ride=51, Tom Low=45, Tom Mid=47, Tom High=50.
                            - Bass on Channel 0 (MIDI Ch 1): Bass notes pitch 28 to 48 (e.g., C1-C3).
                            - Chords on Channel 1 (MIDI Ch 2): Keyboard/Guitar triads/7ths pitch 48 to 72 (e.g., C3-C5).
                            - Melody on Channel 2 (MIDI Ch 3): Lead hook line pitch 60 to 84 (e.g., C4-C6).
                            Ensure note startBeat and durationBeats are exact quarter-note numbers (e.g. 0.0, 0.5, 1.0, 1.5, 2.0, etc.), spanning 4 full bars (16 beats total).
                        """.trimIndent())
                    }
                    put(partObj)
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstruction)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("Gemini API Error (${response.code}): $errorBody")
        }

        val responseString = response.body?.string()
            ?: throw Exception("Empty response body from Gemini API")

        val responseJsonObject = JSONObject(responseString)
        val candidates = responseJsonObject.optJSONArray("candidates")
            ?: throw Exception("No candidates returned in Gemini API response")

        if (candidates.length() == 0) {
            throw Exception("Candidate array empty in Gemini response")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val firstPart = parts.getJSONObject(0)
        val jsonText = firstPart.getString("text")

        // Parse extracted JSON string into SongArrangement
        val cleanedJson = cleanJsonString(jsonText)
        return arrangementAdapter.fromJson(cleanedJson)
            ?: throw Exception("Failed to parse arrangement JSON into SongArrangement model")
    }

    private fun cleanJsonString(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json").trim()
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```").trim()
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```").trim()
        }
        return trimmed
    }
}
