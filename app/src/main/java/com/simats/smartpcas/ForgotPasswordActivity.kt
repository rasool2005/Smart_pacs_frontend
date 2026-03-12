package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.etEmail)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSendCode).setOnClickListener {

            val email = emailInput.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ForgotPasswordRequest(email)

            lifecycleScope.launch {

                try {

                    val response = ApiClient.apiService.sendOtp(request)

                    if (response.isSuccessful) {

                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "OTP Sent to Email",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(
                            this@ForgotPasswordActivity,
                            OtpVerificationActivity::class.java
                        )
                        intent.putExtra("email", email)
                        startActivity(intent)

                    } else {

                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "User not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Server Error",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }
}