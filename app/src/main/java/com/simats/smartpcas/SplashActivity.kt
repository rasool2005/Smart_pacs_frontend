package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay slightly for smooth UX, then check credentials
        Handler(Looper.getMainLooper()).postDelayed({
            checkAutoLogin()
        }, 1500)
    }

    private fun checkAutoLogin() {
        val sessionManager = SessionManager(this)
        val (email, password) = sessionManager.getCredentials()

        if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
            // Credentials exist, verify with backend
            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.loginUser(LoginRequest(email, password))
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // Success: Go to Home
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                    } else {
                        // Failed: Go to Login
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                } catch (e: Exception) {
                    // Error: Go to Login
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                } finally {
                    finish()
                }
            }
        } else {
            // No credentials, check if onboarding was seen
            if (sessionManager.hasSeenOnboarding()) {
                startActivity(Intent(this, HospitalSelectionActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }
    }
}
