package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = checkNotNull(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)) {
            "etEmail is missing or has incorrect type in activity_login.xml"
        }
        val etPassword = checkNotNull(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)) {
            "etPassword is missing or has incorrect type in activity_login.xml"
        }
        val btnLogin = checkNotNull(findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogin)) {
            "btnLogin is missing or has incorrect type in activity_login.xml"
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }

        val tvForgotPassword = checkNotNull(findViewById<android.widget.TextView>(R.id.tvForgotPassword)) {
            "tvForgotPassword is missing in activity_login.xml"
        }
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        val tvSignUp = checkNotNull(findViewById<android.widget.TextView>(R.id.tvSignUp)) {
            "tvSignUp is missing in activity_login.xml"
        }
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.loginUser(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.status == "success" && loginResponse.user != null) {
                        val sessionManager = SessionManager(this@LoginActivity)
                        sessionManager.saveLoginState(true)
                        sessionManager.saveCredentials(email, password)
                        
                        sessionManager.saveUserDetails(
                            loginResponse.user.user_id,
                            loginResponse.user.name,
                            loginResponse.user.email
                        )

                        Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, loginResponse?.message ?: "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Show actual error code for debugging
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LoginActivity, "Server Error ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
