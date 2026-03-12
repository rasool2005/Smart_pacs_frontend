package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val email = intent.getStringExtra("email") ?: ""

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnVerify).setOnClickListener {
            val otp = getOtpFromInputs()
            if (otp.length < 6) {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(email, otp)
        }
    }

    private fun getOtpFromInputs(): String {
        val o1 = findViewById<EditText>(R.id.otp1).text.toString()
        val o2 = findViewById<EditText>(R.id.otp2).text.toString()
        val o3 = findViewById<EditText>(R.id.otp3).text.toString()
        val o4 = findViewById<EditText>(R.id.otp4).text.toString()
        val o5 = findViewById<EditText>(R.id.otp5).text.toString()
        val o6 = findViewById<EditText>(R.id.otp6).text.toString()
        return o1 + o2 + o3 + o4 + o5 + o6
    }

    private fun verifyOtp(email: String, otp: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.verifyOtp(VerifyOtpRequest(email, otp))
                if (response.isSuccessful) {
                    val intent = Intent(this@OtpVerificationActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("otp", otp)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@OtpVerificationActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OtpVerificationActivity, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
