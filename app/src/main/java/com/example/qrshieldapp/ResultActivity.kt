package com.example.qrshieldapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvPrediction: TextView = findViewById(R.id.tvPrediction)
        val tvUrlDisplay: TextView = findViewById(R.id.tvUrlDisplay)
        val btnProceed: Button = findViewById(R.id.btnProceed)

        val scannedUrl = intent.getStringExtra("SCANNED_URL") ?: ""
        val isMalicious = intent.getBooleanExtra("IS_MALICIOUS", false)

        Log.d("QRScanner", "Scanned URL: $scannedUrl")
        tvUrlDisplay.text = scannedUrl // Display scanned URL

        if (isMalicious) {
            // Directly mark as malicious
            tvPrediction.text = "⚠️ This URL is Malicious!"
            tvPrediction.setTextColor(Color.RED)
            btnProceed.text = "Blocked"
            btnProceed.setBackgroundColor(Color.GRAY)
            btnProceed.isEnabled = false
            btnProceed.visibility = Button.VISIBLE
        } else {
            try {
                val model = TFLiteModel(this)
                val features = extractFeatures(scannedUrl)
                val prediction = model.predict(features)

                val resultText = if (prediction > 0.5) "Malicious" else "Safe"

                if (resultText == "Malicious") {
                    tvPrediction.text = "⚠️ This URL is Malicious!"
                    tvPrediction.setTextColor(Color.RED)
                    btnProceed.text = "Blocked: Malicious URL"
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
                btnProceed.visibility = Button.VISIBLE

            } catch (e: Exception) {
                Log.e("ResultActivity", "Error running TFLite model: ${e.message}")
                tvPrediction.text = "❌ Error: Unable to process the URL."
                tvPrediction.setTextColor(Color.DKGRAY)
                btnProceed.visibility = Button.GONE
            }
        }
    }

    private fun extractFeatures(url: String): FloatArray {
        return floatArrayOf(
            url.length.toFloat(),
            url.count { it == '/' }.toFloat(),
            url.count { it == '.' }.toFloat()
        )
    }
}
