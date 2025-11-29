package com.example.qrshieldapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {

    private val TAG = "ResultActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvPrediction: TextView = findViewById(R.id.tvPrediction)
        val tvUrlDisplay: TextView = findViewById(R.id.tvUrlDisplay)
        val tvDebugInfo: TextView = findViewById(R.id.tvDebugInfo)
        val btnProceed: Button = findViewById(R.id.btnProceed)

        val cardGemini: View = findViewById(R.id.cardGemini)
        val tvGeminiCategory: TextView = findViewById(R.id.tvGeminiCategory)
        val tvGeminiKeywords: TextView = findViewById(R.id.tvGeminiKeywords)
        val tvGeminiSummary: TextView = findViewById(R.id.tvGeminiSummary)
        val tvGeminiRisk: TextView = findViewById(R.id.tvGeminiRisk)

        val scannedUrl = intent.getStringExtra("SCANNED_URL") ?: ""
        val isMaliciousIntent = intent.getBooleanExtra("IS_MALICIOUS", false)

        tvUrlDisplay.text = scannedUrl
        tvPrediction.text = "Analyzing..."
        tvPrediction.setTextColor(Color.DKGRAY)
        btnProceed.visibility = Button.GONE
        cardGemini.visibility = View.GONE
        tvDebugInfo.text = ""

        Log.d(TAG, "Scanned URL: $scannedUrl | flaggedIntent: $isMaliciousIntent")

        // If scanner already flagged, show immediate malicious UI but DO NOT return.
        // We set shortCircuited = true so coroutine knows to respect that but still run summary logic.
        val shortCircuited = isMaliciousIntent
        if (shortCircuited) {
            tvPrediction.text = "⚠️ This URL is Malicious!"
            tvPrediction.setTextColor(Color.RED)
            btnProceed.text = "Blocked"
            btnProceed.setBackgroundColor(Color.GRAY)
            btnProceed.isEnabled = false
            btnProceed.visibility = Button.VISIBLE
            // DO NOT return — continue to run ML / SafeBrowsing / Gemini so card can be populated
        }

        lifecycleScope.launch {
            try {
                // 1) ML model prediction (optional)
                val model = try { TFLiteModel(this@ResultActivity) } catch (e: Exception) { null }
                val features = extractFeatures(scannedUrl)
                val prediction = try { model?.predict(features) ?: 0.0f } catch (e: Exception) { 0.0f }
                val mlIsMalicious = prediction > 0.5f

                // 2) Safe Browsing check
                val apiIsMalicious = try {
                    SafeBrowsingHelper.isUrlMalicious(this@ResultActivity, scannedUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "SafeBrowsingHelper exception: ${e.message}", e)
                    false
                }

                // 3) Gemini summary/classification (remote or heuristic)
                val geminiResult = try {
                    GeminiHelper.summarizeUrl(scannedUrl, getString(R.string.gemini_api_key))
                } catch (e: Exception) {
                    Log.e(TAG, "GeminiHelper exception: ${e.message}", e)
                    null
                }

                // ALWAYS populate the Gemini card (show heuristic if geminiResult null)
                if (geminiResult != null) {
                    cardGemini.visibility = View.VISIBLE
                    tvGeminiCategory.text = "Category: ${geminiResult.category}"
                    tvGeminiKeywords.text = "Keywords: ${geminiResult.keywords.joinToString(", ").ifEmpty { "—" }}"
                    tvGeminiSummary.text = geminiResult.summary.ifEmpty { "No summary returned." }
                    tvGeminiRisk.text = "Risk: ${"%.2f".format(geminiResult.risk)}"
                } else {
                    // Show fallback heuristic summary so the card appears for malicious URLs too
                    val (heuristicSummary, keywords) = GeminiHelper.heuristicSummary(scannedUrl)
                    cardGemini.visibility = View.VISIBLE
                    tvGeminiCategory.text = "Category: heuristic"
                    tvGeminiKeywords.text = "Keywords: ${keywords.joinToString(", ").ifEmpty { "—" }}"
                    tvGeminiSummary.text = heuristicSummary
                    tvGeminiRisk.text = "Risk: 0.15"
                }

                // 4) Final decision logic (priority: SafeBrowsing > Gemini > ML)
                val geminiMalicious = geminiResult?.category?.equals("malicious", true) ?: false
                val geminiDeface = geminiResult?.category?.equals("defacement", true) ?: false

                // If scanner short-circuited, keep finalIsMalicious true; else compute normally
                val finalIsMalicious = shortCircuited || apiIsMalicious || geminiMalicious || geminiDeface || mlIsMalicious

                // Update top verdict UI only if not short-circuited (we already showed malicious)
                if (!shortCircuited) {
                    if (finalIsMalicious) {
                        tvPrediction.text = "⚠️ This URL is Malicious!"
                        tvPrediction.setTextColor(Color.RED)
                        btnProceed.text = "Blocked"
                        btnProceed.setBackgroundColor(Color.GRAY)
                        btnProceed.isEnabled = false
                    } else {
                        tvPrediction.text = "✅ This URL is Safe"
                        tvPrediction.setTextColor(Color.parseColor("#4CAF50"))
                        btnProceed.text = "Proceed to Site"
                        btnProceed.setBackgroundColor(Color.parseColor("#4CAF50"))
                        btnProceed.isEnabled = true
                        btnProceed.setOnClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scannedUrl)))
                        }
                    }
                } else {
                    // short-circuited: still allow showing gemini result and saving history but keep blocked button disabled
                    btnProceed.isEnabled = false
                    btnProceed.text = "Blocked"
                }

                val debug = "ML: ${"%.2f".format(prediction)} | API: $apiIsMalicious | Gemini: ${geminiResult?.category ?: "heuristic"}"
                tvDebugInfo.text = debug
                Log.d(TAG, "Final decision: finalIsMalicious=$finalIsMalicious ; $debug")

                // Save to Firestore if user signed in (same as before)
                try {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val uid = user.uid
                        val repo = FirebaseRepository()
                        val toSave = HistoryItem(
                            url = scannedUrl,
                            isMalicious = finalIsMalicious,
                            mlScore = prediction,
                            apiResult = apiIsMalicious,
                            geminiCategory = geminiResult?.category ?: "heuristic",
                            geminiSummary = geminiResult?.summary ?: GeminiHelper.heuristicSummary(scannedUrl).first,
                            userId = uid
                        )
                        lifecycleScope.launch {
                            try {
                                repo.saveHistoryItem(uid, toSave)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to save history: ${e.message}", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore save attempt failed: ${e.message}", e)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}", e)
            }
        }
    }

    // ... extractFeatures & helpers unchanged ...
    // -------------------------
//   ML Feature Extractor
// -------------------------
    private fun extractFeatures(url: String): FloatArray {
        return floatArrayOf(
            url.length.toFloat(),                              // feature 1
            url.count { it == '/' }.toFloat(),                 // feature 2
            url.count { it == '.' }.toFloat(),                 // feature 3
            url.count { it == '-' }.toFloat(),                 // feature 4
            url.count { it == '@' }.toFloat(),                 // feature 5
            if (url.startsWith("https")) 1f else 0f,           // feature 6
            url.count { it.isDigit() }.toFloat(),              // feature 7
            getDomainLength(url).toFloat(),                    // feature 8
            getTldLength(url).toFloat(),                       // feature 9
            getSubdomainLength(url).toFloat()                  // feature 10
        )
    }

    // -------------------------
//   URL Parsing Helpers
// -------------------------
    private fun getDomainLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val parts = host.split(".")
            if (parts.size < 2) 0 else parts[parts.size - 2].length
        } catch (e: Exception) {
            0
        }
    }

    private fun getTldLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val parts = host.split(".")
            if (parts.size < 2) 0 else parts.last().length
        } catch (e: Exception) {
            0
        }
    }

    private fun getSubdomainLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val parts = host.split(".")
            if (parts.size <= 2) 0 else parts.dropLast(2).joinToString(".").length
        } catch (e: Exception) {
            0
        }
    }

}
