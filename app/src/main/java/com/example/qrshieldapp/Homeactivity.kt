package com.example.qrshieldapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homeactivity)

        val scanButton = findViewById<Button>(R.id.btn_scan)
        val pasteButton = findViewById<Button>(R.id.btn_paste)
        val urlInput = findViewById<EditText>(R.id.et_url)

        // Open QR Scanner (MainActivity)
        scanButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Handle pasted URL
        pasteButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                Toast.makeText(this, "URL Entered: $url", Toast.LENGTH_SHORT).show()
                // Process the URL (e.g., check for phishing)
            } else {
                Toast.makeText(this, "Please enter a URL!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}