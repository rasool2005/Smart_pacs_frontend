package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        // Language Selection
        findViewById<android.view.View>(R.id.btnLanguage).setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Privacy Settings
        findViewById<android.view.View>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, DataPrivacyActivity::class.java))
        }

        // Feedback
        findViewById<android.view.View>(R.id.btnFeedback).setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
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
        // Set profile as selected since Settings is part of the Profile section
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
                    // Already in profile section (via Settings), but if they click Profile icon,
                    // we might want to go back to ProfileActivity or just stay here.
                    // Usually, ProfileActivity is the parent.
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = LanguageHelper.getSortedLanguages()
        val languageNames = languages.map { it.getDisplayLanguage(java.util.Locale.ENGLISH) }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languageNames) { _, which ->
                val selectedLocale = languages[which]
                val languageCode = selectedLocale.language

                // Save Preference
                SessionManager(this).saveLanguage(languageCode)

                // Apply Locale immediately for this context if needed
                LanguageHelper.setLocale(this, languageCode)

                // Restart App to apply changes globally
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
