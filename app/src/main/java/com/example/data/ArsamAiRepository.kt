package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "ArsamAiRepo"

data class RequestCheckReport(
    val missingFields: List<String>,
    val suspiciousOrDuplicateHint: String,
    val summaryTip: String
)

class ArsamAiRepository {

    private val systemPrompt = """
        You are the ARSAM Guide Assistant for a community blood donor coordination app.
        STRICT SAFETY & PRIVACY RULES:
        1. NEVER diagnose diseases, prescribe treatments, confirm medical eligibility to donate blood, or determine transfusion compatibility/crossmatching. Always state that qualified medical professionals and authorized blood banks make all clinical decisions.
        2. NEVER ask for or repeat personal phone numbers, street addresses, or patient names.
        3. Support English, Kannada (ಕನ್ನಡ), and Malayalam (മലയാളം) clearly and politely.
        4. Help users navigate ARSAM features (Live Pulse availability, Rescue Chain alerts, Trust Gate verification, Blood Freshness inventory, Safe Contact consent, Community Gap Map) and explain general WHO/NBTC-style voluntary blood donation steps clearly.
    """.trimIndent()

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    private val requestCheckSchema = Schema.obj(
        properties = mapOf(
            "missingFields" to Schema.array(
                items = Schema.string(),
                description = "Non-PII fields missing or unclear in the blood request draft"
            ),
            "suspiciousOrDuplicateHint" to Schema.string(
                description = "Human-review guidance if request appears incomplete, duplicate, or unusual"
            ),
            "summaryTip" to Schema.string(
                description = "Helpful non-medical tip in the requested language"
            )
        )
    )

    private val structuredCheckModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = requestCheckSchema
            }
        )
    }

    fun streamAssistantReply(userPrompt: String, languageLabel: String): Flow<String> = flow {
        val sanitizedPrompt = userPrompt.trim().take(800)
        val composedPrompt = "Preferred response language: $languageLabel.\nUser question: $sanitizedPrompt"
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(composedPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Guide returned an empty response.")
        }
    }

    suspend fun checkEmergencyDraft(
        bloodGroup: String,
        units: String,
        hospital: String,
        district: String,
        requiredBy: String,
        description: String,
        existingActiveCountSameFingerprint: Int,
        languageLabel: String
    ): Result<RequestCheckReport> = try {
        // Strictly send ONLY non-PII draft metadata to Gemini
        val prompt = """
            Review this anonymized emergency blood request draft for completeness and potential duplicate/human-review flags.
            Respond in $languageLabel inside the JSON fields.
            - Blood group: $bloodGroup
            - Units requested: $units
            - Hospital / facility name: ${hospital.ifBlank { "(blank)" }}
            - District: $district
            - Required date/time: ${requiredBy.ifBlank { "(blank)" }}
            - Non-PII description: ${description.ifBlank { "(blank)" }}
            - Active requests already matching same blood group + hospital/district: $existingActiveCountSameFingerprint
        """.trimIndent()

        val response = withTimeout(60_000L) {
            structuredCheckModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty JSON response from Gemini.")
        val obj = JSONObject(rawJson)
        val missingArr = obj.optJSONArray("missingFields")
        val missing = buildList {
            if (missingArr != null) {
                for (i in 0 until missingArr.length()) {
                    add(missingArr.optString(i))
                }
            }
        }
        Result.success(
            RequestCheckReport(
                missingFields = missing,
                suspiciousOrDuplicateHint = obj.optString("suspiciousOrDuplicateHint", "No flags detected."),
                summaryTip = obj.optString("summaryTip", "Ensure hospital coordinator details are verified.")
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(AI_TAG, "Request draft check timed out", e)
        Result.failure(Exception("AI draft check timed out after 60 seconds."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AI_TAG, "Request draft check failed", e)
        Result.failure(e)
    }
}
