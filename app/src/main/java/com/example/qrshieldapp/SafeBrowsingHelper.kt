package com.example.qrshieldapp

import android.content.Context
import android.util.Log

object SafeBrowsingHelper {

    private const val TAG = "SafeBrowsingHelper"
    private val repo = SafeBrowsingRepository()

    /**
     * A simple wrapper that:
     * 1) Reads the API key from strings.xml
     * 2) Calls SafeBrowsingRepository.isUrlMalicious()
     *
     * @param context Needed to access strings.xml
     * @param url URL to check
     * @return Boolean (true = malicious)
     */
    suspend fun isUrlMalicious(context: Context, url: String): Boolean {
        return try {
            val apiKey = context.getString(R.string.gsb_api_key)

            if (apiKey.isBlank()) {
                Log.e(TAG, "GSB API KEY is missing from strings.xml")
                return false
            }

            repo.isUrlMalicious(url, apiKey)
        } catch (e: Exception) {
            Log.e(TAG, "SafeBrowsingHelper error: ${e.message}", e)
            false
        }
    }
}
