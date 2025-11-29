package com.example.qrshieldapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*

data class GeminiSummary(
    val category: String,
    val keywords: List<String>,
    val summary: String,
    val risk: Float
)

object GeminiHelper {
    private const val TAG = "GeminiHelper"
    private val client = OkHttpClient()

    /**
     * Try remote LLM (if apiKey present) else fallback to heuristic.
     * Returns GeminiSummary (never null).
     */
    suspend fun summarizeUrl(url: String, apiKey: String?): GeminiSummary {
        // 1) try remote
        if (!apiKey.isNullOrBlank()) {
            try {
                val res = summarizeUrlRemote(url, apiKey)
                if (res != null) return res
            } catch (e: Exception) {
                Log.w(TAG, "Remote Gemini call failed: ${e.message}")
            }
        }

        // 2) heuristic fallback
        val (summary, keywords) = heuristicSummary(url)
        val cat = when {
            keywords.contains("login") || keywords.contains("payment") -> "malicious"
            keywords.contains("defacement") -> "defacement"
            else -> "safe"
        }
        val risk = when (cat) {
            "malicious" -> 0.85f
            "defacement" -> 0.6f
            else -> 0.12f
        }
        return GeminiSummary(cat, keywords, summary, risk)
    }

    // Remote call to a cloud LLM (example: Google Generative Language API text-bison)
    // Returns GeminiSummary or null on failure
    suspend fun summarizeUrlRemote(url: String, apiKey: String): GeminiSummary? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(url)
                val endpoint = "https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generate?key=${URLEncoder.encode(apiKey, "UTF-8")}"

                val bodyJson = JSONObject()
                bodyJson.put("prompt", JSONObject().put("text", prompt))
                bodyJson.put("maxOutputTokens", 256)

                val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), bodyJson.toString())
                val req = Request.Builder().url(endpoint).post(body).build()

                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string() ?: ""
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "LLM remote call failed: ${resp.code} ${resp.message} | body=$text")
                        return@withContext null
                    }
                    // Try parse common shapes
                    try {
                        val jo = JSONObject(text)
                        // try candidates -> content
                        if (jo.has("candidates")) {
                            val c = jo.getJSONArray("candidates")
                            if (c.length() > 0) {
                                val first = c.getJSONObject(0)
                                val content = first.optString("content", first.optString("output", first.optString("text", "")))
                                return@withContext parseJsonToSummary(content)
                            }
                        }
                        // try output/text fields
                        val maybe = jo.optString("output", jo.optString("text", ""))
                        if (maybe.isNotBlank()) {
                            val parsed = parseJsonToSummary(maybe)
                            if (parsed != null) return@withContext parsed
                        }
                        // fallback: extract any JSON substring
                        val subs = extractJsonFromResponse(text)
                        val parsed = parseJsonToSummary(subs)
                        if (parsed != null) return@withContext parsed

                        // last fallback: return raw truncated text
                        val trunc = if (text.length > 500) text.substring(0, 500) else text
                        return@withContext GeminiSummary("safe", listOf(), trunc, 0.2f)
                    } catch (e: Exception) {
                        Log.w(TAG, "Parsing remote LLM response failed: ${e.message}")
                        val trunc = if (text.length > 500) text.substring(0, 500) else text
                        return@withContext GeminiSummary("safe", listOf(), trunc, 0.2f)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "summarizeUrlRemote exception: ${e.message}", e)
                return@withContext null
            }
        }
    }

    private fun parseJsonToSummary(text: String?): GeminiSummary? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.trim()
        val jsonText = if (cleaned.startsWith("{") && cleaned.contains("category")) {
            cleaned
        } else {
            extractJsonFromResponse(cleaned)
        }
        try {
            val j = JSONObject(jsonText)
            val category = j.optString("category", "safe")
            val keywords = mutableListOf<String>()
            if (j.has("keywords")) {
                val a = j.opt("keywords")
                if (a is JSONArray) {
                    for (i in 0 until a.length()) keywords.add(a.optString(i))
                } else if (a is String) {
                    keywords.addAll(a.split(",").map { it.trim() })
                }
            }
            val summary = j.optString("summary", j.optString("text", ""))
            val risk = j.optDouble("risk", 0.2).toFloat()
            return GeminiSummary(category, keywords, summary, risk)
        } catch (e: Exception) {
            Log.w(TAG, "parseJsonToSummary failed: ${e.message}")
            return null
        }
    }

    private fun extractJsonFromResponse(resp: String): String {
        val trimmed = resp.trim()
        val first = trimmed.indexOfFirst { it == '{' }
        val last = trimmed.indexOfLast { it == '}' }
        return if (first >= 0 && last > first) trimmed.substring(first, last + 1) else "{}"
    }

    private fun buildPrompt(url: String): String {
        return """
            You are a web classifier. Return EXACTLY one JSON object with keys:
            {"category":"safe|malicious|defacement","keywords":["k1","k2"],"summary":"one sentence","risk":0.0}
            Category must be one of safe, malicious, defacement.
            Keywords: up to 3 short words inferred from URL.
            Risk: number between 0.0 and 1.0.
            Respond ONLY with the JSON object and nothing else.
            URL: $url
        """.trimIndent()
    }

    // Deterministic heuristic fallback
    fun heuristicSummary(url: String): Pair<String, List<String>> {
        val keywords = ArrayList<String>()
        val lower = url.lowercase(Locale.getDefault())
        if (lower.contains("login") || lower.contains("signin")) keywords.add("login")
        if (lower.contains("bank") || lower.contains("pay") || lower.contains("payment")) keywords.add("payment")
        if (lower.contains("admin") || lower.contains("wp-admin")) keywords.add("admin")
        if (lower.contains("deface") || lower.contains("hacked")) keywords.add("defacement")
        if (lower.contains("http://") && !lower.contains("https://")) keywords.add("no-https")
        if (Regex("""\d{4,}""").containsMatchIn(lower)) keywords.add("numbers")
        val cat = when {
            keywords.contains("login") || keywords.contains("payment") -> "malicious"
            keywords.contains("defacement") -> "defacement"
            else -> "safe"
        }
        val summary = when (cat) {
            "malicious" -> "URL contains login/payment/admin keywords — may attempt credential theft."
            "defacement" -> "URL suggests defacement/hacked content."
            else -> "No obvious malicious patterns detected in the URL."
        }
        val kw = if (keywords.size > 3) keywords.subList(0, 3) else keywords
        return Pair(summary, kw)
    }
}
