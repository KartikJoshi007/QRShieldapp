package com.example.qrshieldapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()

        val CurrentUser:FirebaseUser? = auth.currentUser
        if (CurrentUser != null){

            startActivity(Intent(this, HomeActivity::class.java))
            finish()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val email = findViewById<EditText>(R.id.et_email)
        val password = findViewById<EditText>(R.id.et_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val signupText = findViewById<TextView>(R.id.tv_signup)

        auth = FirebaseAuth.getInstance()


        loginButton.setOnClickListener {

            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if ( mail.isNotEmpty() && pass.isNotEmpty()) {
                // Proceed to HomeActivity (You can implement Firebase authentication here)

                auth.signInWithEmailAndPassword(mail,pass).addOnCompleteListener(this){
                        task->
                    if (task.isSuccessful){
                        Toast.makeText(this, "User Login Successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    else{
                        Toast.makeText(this,"Login Failed : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }


                }
            }
            else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}