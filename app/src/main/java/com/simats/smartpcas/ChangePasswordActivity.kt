package com.simats.smartpcas

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        sessionManager = SessionManager(this)

        // Initialize Views
        val etCurrentPassword = findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnUpdatePassword = findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Back Navigation
        btnBack.setOnClickListener {
            finish()
        }

        // Update Button Logic
        btnUpdatePassword.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString().trim()
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(newPass)) {
                Toast.makeText(this, "Password does not meet requirements", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val userId = sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button and show loading text
            btnUpdatePassword.isEnabled = false
            btnUpdatePassword.text = "Updating..."

            val request = ChangePasswordRequest(
                user_id = userId,
                current_password = currentPass,
                new_password = newPass,
                confirm_password = confirmPass
            )

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.changePassword(request)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.status == "success") {
                            Toast.makeText(this@ChangePasswordActivity, body.message ?: "Password updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            // Extract errors if passed as 200 OK with status="error"
                            val errorMessage = body.errors?.values?.firstOrNull() ?: body.message ?: "An error occurred"
                            Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Handle 400 Bad Request or other HTTP errors
                        val errorString = response.errorBody()?.string()
                        var errorMessage = "Server Error: ${response.code()}"
                        
                        // Try parsing JSON error
                        try {
                            if (!errorString.isNullOrEmpty()) {
                                val jsonObject = JSONObject(errorString)
                                if (jsonObject.has("errors")) {
                                    val errorsObj = jsonObject.getJSONObject("errors")
                                    val keys = errorsObj.keys()
                                    if (keys.hasNext()) {
                                        errorMessage = errorsObj.getString(keys.next())
                                    }
                                } else if (jsonObject.has("message")) {
                                    errorMessage = jsonObject.getString("message")
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChangePasswordActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    // Re-enable button
                    btnUpdatePassword.isEnabled = true
                    btnUpdatePassword.text = "Update Password"
                }
            }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=])(?=\\S+\$).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
