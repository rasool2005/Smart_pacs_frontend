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

        // Faster transition (500ms) for better UX
        Handler(Looper.getMainLooper()).postDelayed({
            checkAutoLogin()
        }, 500)
    }

    private fun checkAutoLogin() {
        val sessionManager = SessionManager(this)
        
        // Instant navigation if session exists (don't block on network)
        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Check if onboarding seen
        if (sessionManager.hasSeenOnboarding()) {
            startActivity(Intent(this, HospitalSelectionActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
