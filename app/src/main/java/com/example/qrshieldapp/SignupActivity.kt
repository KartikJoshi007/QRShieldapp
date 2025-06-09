package com.example.qrshieldapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        val username = findViewById<EditText>(R.id.et_signup_username)
        val email = findViewById<EditText>(R.id.et_signup_email)
        val password = findViewById<EditText>(R.id.et_signup_password)
        val confirmPassword = findViewById<EditText>(R.id.et_signup_confirm_password)
        val registerButton = findViewById<Button>(R.id.btn_signup)
        val loginText = findViewById<TextView>(R.id.tv_login)

        registerButton.setOnClickListener {
            val user = username.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            if (user.isNotEmpty() && mail.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    // TODO: Implement Firebase authentication for user registration
                    auth.createUserWithEmailAndPassword(mail,pass).addOnCompleteListener(this){
                        task->
                        if (task.isSuccessful){
                            Toast.makeText(this, "User Registered Successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        else{
                            Toast.makeText(this,"Registration Failed : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }


                } else {
                    Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}