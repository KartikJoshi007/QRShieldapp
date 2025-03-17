package com.example.qrshieldapp  // Replace with your actual package name

import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.nio.FloatBuffer

class ResultActivity : AppCompatActivity() {
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)  // Ensure this XML file exists

        val tvPrediction: TextView = findViewById(R.id.tvPrediction)
        val btnProceed: Button = findViewById(R.id.btnProceed)

        // Get the scanned URL
        val scannedUrl = intent.getStringExtra("SCANNED_URL") ?: ""

        // Load ONNX Model
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            assets.open("rf_model.onnx").use { modelStream ->
                val modelBytes = modelStream.readBytes()
                val options = OrtSession.SessionOptions()
                ortSession = ortEnv.createSession(modelBytes, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvPrediction.text = "Error: AI model failed to load."
            return
        }

        // Make Prediction
        val prediction = predictUrl(scannedUrl)
        tvPrediction.text = prediction

        // Handle button visibility and action
        btnProceed.visibility = Button.VISIBLE
        if (prediction == "Safe") {
            btnProceed.text = "Proceed to Site"
            btnProceed.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scannedUrl)))
            }
        } else {
            btnProceed.text = "Blocked: Malicious URL"
            btnProceed.isEnabled = false
        }
    }

    private fun predictUrl(url: String): String {
        return try {
            val inputTensor = preprocessUrl(url)

            // Run the model and retrieve output tensor
            val output = ortSession.run(mapOf("input" to inputTensor))
            val outputName = ortSession.outputNames.firstOrNull()
                ?: return "Error: Model output name not found."

            val resultTensor = output[outputName] as? OnnxTensor
                ?: return "Error: Failed to process model output."

            resultTensor.use { tensor ->
                val resultArray = FloatArray(tensor.floatBuffer.remaining())
                tensor.floatBuffer.get(resultArray)
                if (resultArray[0] > 0.5) "Malicious" else "Safe"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Prediction failed."
        }
    }

    private fun preprocessUrl(url: String): OnnxTensor {
        val features = floatArrayOf(
            url.length.toFloat(),
            url.count { it == '/' }.toFloat(),
            url.count { it == '.' }.toFloat()
        )

        return OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(features), longArrayOf(1, features.size.toLong()))
    }

    override fun onDestroy() {
        super.onDestroy()
        ortSession.close()
        ortEnv.close()
    }
}
