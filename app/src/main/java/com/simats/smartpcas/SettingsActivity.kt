package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val sessionManager = SessionManager(this)

        // Dark Mode Switch
        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        switchDarkMode.isChecked = sessionManager.isDarkMode()
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setDarkMode(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Recreate to apply theme immediately
            recreate()
        }

        // Privacy Settings
        findViewById<android.view.View>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, DataPrivacyActivity::class.java))
        }

        // Terms
        findViewById<android.view.View>(R.id.btnTerms).setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }

        // About App
        findViewById<android.view.View>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Handle bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_patients -> {
                    startActivity(Intent(this, PatientsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_ai_chat -> {
                    startActivity(Intent(this, AiChatActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_schedule -> {
                    startActivity(Intent(this, FollowUpActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
