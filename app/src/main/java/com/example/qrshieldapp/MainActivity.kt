package com.example.qrshieldapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*

class MainActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private val CAMERA_PERMISSION_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)

        codeScanner = CodeScanner(this, scannerView).apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
        }

        // Handle QR Code Scan Results
        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                val scannedUrl = result.text
                Log.d("QRScanner", "Scanned URL: $scannedUrl")
                Toast.makeText(this, "Scanned: $scannedUrl", Toast.LENGTH_LONG).show()

                // Logic to mark URL as malicious or pass to ML model
                val isMalicious = scannedUrl.contains("http") && !scannedUrl.contains("https") || scannedUrl.contains("8080")

                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("SCANNED_URL", scannedUrl)
                intent.putExtra("IS_MALICIOUS", isMalicious) // Pass status to ResultActivity
                startActivity(intent)
            }
        }

        // Handle Errors
        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Log.e("QRScanner", "Camera error: ${it.message}")
                Toast.makeText(this, "Camera initialization error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Start scanning when clicking on the scanner view
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }

        // Zoom feature using SeekBar
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                codeScanner.zoom = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    // Function to check and request camera permissions
    private fun checkPermission(permission: String, reqCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), reqCode)
        }
    }
}
