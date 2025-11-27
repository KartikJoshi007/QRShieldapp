package com.example.qrshieldapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SafeBrowsingRepository(
    private val service: SafeBrowsingApiService = RetrofitClient.instance.create(SafeBrowsingApiService::class.java)
) {
    companion object { private const val TAG = "SafeBrowsingRepo" }

    /**
     * Returns true if Google Safe Browsing returns matches for the provided URL.
     * Call this function from a coroutine (e.g., lifecycleScope.launch).
     *
     * @param url The URL to check
     * @param apiKey The Google Safe Browsing API key (read from strings.xml)
     */
    suspend fun isUrlMalicious(url: String, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    Log.e(TAG, "GSB API key missing (empty).")
                    return@withContext false
                }

                val request = ThreatMatchesRequest(
                    client = ClientInfo(clientId = "qrshieldapp", clientVersion = "1.0"),
                    threatInfo = ThreatInfo(
                        threatTypes = listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"),
                        platformTypes = listOf("ANY_PLATFORM"),
                        threatEntryTypes = listOf("URL"),
                        threatEntries = listOf(ThreatEntry(url))
                    )
                )

                val response = service.findThreatMatches(apiKey, request)

                if (!response.isSuccessful) {
                    val errBody = response.errorBody()?.string()
                    Log.e(TAG, "SafeBrowsing API error: code=${response.code()}, body=$errBody")
                    return@withContext false
                }

                val body = response.body()
                val matches = body?.matches
                val malicious = matches != null && matches.isNotEmpty()
                Log.d(TAG, "SafeBrowsing matches: $malicious ; matchCount=${matches?.size ?: 0}")
                return@withContext malicious
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception: ${e.message}", e)
                return@withContext false
            } catch (t: Throwable) {
                Log.e(TAG, "SafeBrowsing request failed: ${t.message}", t)
                return@withContext false
            }
        }
    }
}
