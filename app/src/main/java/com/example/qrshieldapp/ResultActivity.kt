package com.example.qrshieldapp

import android.content.Intent
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
        val btnProceed: Button = findViewById(R.id.btnProceed)

        val scannedUrl = intent.getStringExtra("SCANNED_URL") ?: ""
        val isMalicious = intent.getBooleanExtra("IS_MALICIOUS", false)

        Log.d("QRScanner", "Scanned URL: $scannedUrl")

        if (isMalicious) {
            // Directly mark as malicious
            tvPrediction.text = "⚠️ This URL is Malicious!"
            btnProceed.text = "Blocked"
            btnProceed.isEnabled = false
        } else {
            // Process URL with ML Model
            try {
                val model = TFLiteModel(this)
                val features = extractFeatures(scannedUrl)
                val prediction = model.predict(features)

                val resultText = if (prediction > 0.5) "Malicious" else "Safe"
                tvPrediction.text = resultText

                btnProceed.visibility = Button.VISIBLE
                if (resultText == "Safe") {
                    btnProceed.text = "Proceed to Site"
                    btnProceed.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scannedUrl)))
                    }
                } else {
                    btnProceed.text = "Blocked: Malicious URL"
                    btnProceed.isEnabled = false
                }
            } catch (e: Exception) {
                Log.e("ResultActivity", "Error running TFLite model: ${e.message}")
                tvPrediction.text = "Error: Unable to process the URL."
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
