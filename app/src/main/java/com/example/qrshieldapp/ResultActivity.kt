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
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private val TAG = "ResultActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvPrediction: TextView = findViewById(R.id.tvPrediction)
        val tvUrlDisplay: TextView = findViewById(R.id.tvUrlDisplay)
        val tvDebugInfo: TextView = findViewById(R.id.tvDebugInfo)
        val btnProceed: Button = findViewById(R.id.btnProceed)

        // Gemini card views
        val cardGemini = findViewById<View>(R.id.cardGemini)
        val tvGeminiCategory: TextView = findViewById(R.id.tvGeminiCategory)
        val tvGeminiKeywords: TextView = findViewById(R.id.tvGeminiKeywords)
        val tvGeminiSummary: TextView = findViewById(R.id.tvGeminiSummary)
        val tvGeminiRisk: TextView = findViewById(R.id.tvGeminiRisk)

        val scannedUrl = intent.getStringExtra("SCANNED_URL") ?: ""
        val isMaliciousIntent = intent.getBooleanExtra("IS_MALICIOUS", false)

        // Read keys from strings.xml (development approach)
        val gsbKey = getString(R.string.gsb_api_key)
        val geminiKey = getString(R.string.gemini_api_key)
        val geminiProvider = getString(R.string.gemini_provider)

        tvUrlDisplay.text = scannedUrl
        tvPrediction.text = "Analyzing..."
        tvPrediction.setTextColor(Color.DKGRAY)
        btnProceed.visibility = Button.GONE
        cardGemini.visibility = View.GONE
        tvDebugInfo.text = ""

        Log.d(TAG, "Scanned URL: $scannedUrl | intentFlag: $isMaliciousIntent")

        // Quick short-circuit if scanner already flagged as malicious
        if (isMaliciousIntent) {
            tvPrediction.text = "⚠️ This URL is Malicious!"
            tvPrediction.setTextColor(Color.RED)
            btnProceed.text = "Blocked"
            btnProceed.setBackgroundColor(Color.GRAY)
            btnProceed.isEnabled = false
            btnProceed.visibility = Button.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                // 1) On-device ML prediction
                val model = TFLiteModel(this@ResultActivity)
                val features = extractFeatures(scannedUrl)
                Log.d(TAG, "Features -> ${features.joinToString()}")
                val prediction = model.predict(features) // expected float 0..1
                Log.d(TAG, "ML model prediction: $prediction")
                val mlIsMalicious = prediction > 0.5f

                // 2) Safe Browsing check using SafeBrowsingHelper (reads key from strings.xml via context)
                val apiIsMalicious = try {
                    SafeBrowsingHelper.isUrlMalicious(this@ResultActivity, scannedUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "SafeBrowsingHelper exception: ${e.message}", e)
                    false // fail-open: treat as safe on helper failure (change if you want fail-closed)
                }
                Log.d(TAG, "SafeBrowsing API result: $apiIsMalicious")

                // 3) Gemini LLM classification & summary (pass geminiKey and provider)
                val geminiResult = try {
                    GeminiHelper.summarizeUrl(scannedUrl, geminiKey, geminiProvider)
                } catch (e: Exception) {
                    Log.e(TAG, "GeminiHelper exception: ${e.message}", e)
                    null
                }

                // Populate Gemini card if available
                if (geminiResult != null) {
                    cardGemini.visibility = View.VISIBLE
                    tvGeminiCategory.text = "Category: ${geminiResult.category}"
                    tvGeminiKeywords.text = "Keywords: ${geminiResult.keywords.joinToString(", ")}"
                    tvGeminiSummary.text = geminiResult.summary
                    tvGeminiRisk.text = "Risk: ${String.format("%.2f", geminiResult.risk)}"
                } else {
                    cardGemini.visibility = View.GONE
                }

                // 4) Final decision logic (priority: API > Gemini > ML)
                val geminiMalicious = geminiResult?.category?.equals("malicious", true) ?: false
                val geminiDeface = geminiResult?.category?.equals("defacement", true) ?: false
                val finalIsMalicious = apiIsMalicious || geminiMalicious || geminiDeface || mlIsMalicious

                // Update UI accordingly
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

                // Debug info (shows raw model score, api, gemini)
                val debug = "ML: ${"%.2f".format(prediction)} | API: $apiIsMalicious | Gemini: ${geminiResult?.category ?: "n/a"}"
                tvDebugInfo.text = debug
                Log.d(TAG, "Final decision: finalIsMalicious=$finalIsMalicious ; $debug")

                btnProceed.visibility = Button.VISIBLE

            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}", e)
                tvPrediction.text = "❌ Error: Unable to analyze the URL."
                tvPrediction.setTextColor(Color.DKGRAY)
                btnProceed.visibility = Button.GONE
                cardGemini.visibility = View.GONE
            }
        }
    }

    private fun extractFeatures(url: String): FloatArray {
        // Match the features & order used during training
        return floatArrayOf(
            url.length.toFloat(),
            url.count { it == '/' }.toFloat(),
            url.count { it == '.' }.toFloat(),
            url.count { it == '-' }.toFloat(),
            url.count { it == '@' }.toFloat(),
            if (url.startsWith("https")) 1f else 0f,
            url.count { it.isDigit() }.toFloat(),
            getDomainLength(url).toFloat(),
            getTldLength(url).toFloat(),
            getSubdomainLength(url).toFloat()
        )
    }

    private fun getDomainLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val domainParts = host.split(".")
            if (domainParts.size >= 2) domainParts[domainParts.size - 2].length else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getTldLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val domainParts = host.split(".")
            if (domainParts.size >= 2) domainParts.last().length else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getSubdomainLength(url: String): Int {
        return try {
            val host = Uri.parse(url).host ?: return 0
            val domainParts = host.split(".")
            if (domainParts.size > 2) domainParts.dropLast(2).joinToString(".").length else 0
        } catch (e: Exception) {
            0
        }
    }
}
