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
    private var selectedHospitalId: String? = null
    private var selectedHospitalName: String? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)
        selectedHospitalId = intent.getStringExtra("hospital_id")
        selectedHospitalName = intent.getStringExtra("hospital_name")

        // If not passed via intent, check session manager
        if (selectedHospitalId == null) {
            selectedHospitalId = sessionManager.getSelectedHospitalId()
            selectedHospitalName = sessionManager.getSelectedHospitalName()
        } else {
            // Save if it was passed via intent
            sessionManager.saveSelectedHospital(selectedHospitalId!!, selectedHospitalName ?: "Hospital")
        }

        val tvSubtitle = findViewById<android.widget.TextView>(R.id.tvSubtitle)
        // Reverted to original static text
        tvSubtitle.text = "Login to continue your diagnosis"

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
                // Force logout/clear of previous session data before new login
                sessionManager.logout()
                
                // Restore selected hospital after logout clears it
                selectedHospitalId?.let { id ->
                    selectedHospitalName?.let { name ->
                        sessionManager.saveSelectedHospital(id, name)
                    }
                }
                
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
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("hospital_id", selectedHospitalId)
            intent.putExtra("hospital_name", selectedHospitalName)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.loginUser(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.status == "success" && loginResponse.user != null) {
                        
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
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            val jsonObject = org.json.JSONObject(errorBody)
                            var errorMessage = "Login Failed"
                            
                            if (jsonObject.has("message")) {
                                val messageObj = jsonObject.opt("message")
                                if (messageObj is org.json.JSONObject) {
                                    if (messageObj.has("non_field_errors")) {
                                        val errors = messageObj.getJSONArray("non_field_errors")
                                        if (errors.length() > 0) {
                                            errorMessage = errors.getString(0)
                                        }
                                    } else {
                                        val keys = messageObj.keys()
                                        if (keys.hasNext()) {
                                            val key = keys.next()
                                            val errors = messageObj.optJSONArray(key)
                                            if (errors != null && errors.length() > 0) {
                                                errorMessage = errors.getString(0)
                                            }
                                        }
                                    }
                                } else if (messageObj is String) {
                                    errorMessage = messageObj
                                }
                            }
                            Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Server Error ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
