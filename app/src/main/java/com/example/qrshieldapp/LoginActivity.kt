package com.example.qrshieldapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.et_username)
        val email = findViewById<EditText>(R.id.et_email)
        val password = findViewById<EditText>(R.id.et_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val signupText = findViewById<TextView>(R.id.tv_signup)

        loginButton.setOnClickListener {
            val user = username.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if (user.isNotEmpty() && mail.isNotEmpty() && pass.isNotEmpty()) {
                // Proceed to HomeActivity (You can implement Firebase authentication here)
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}