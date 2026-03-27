package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
    }

    private fun applyTheme() {
        val sessionManager = SessionManager(this)
        if (sessionManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onResume() {
        super.onResume()
        // It's a good practice to update the nav selection when the activity resumes
        if (this is HomeActivity || this is PatientsActivity || this is AiChatActivity || this is FollowUpActivity || this is ProfileActivity) {
            setupBottomNavigation()
        }
    }

    open fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            if (this !is HomeActivity) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        findViewById<LinearLayout>(R.id.navPatients).setOnClickListener {
            if (this !is PatientsActivity) {
                val intent = Intent(this, PatientsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        findViewById<LinearLayout>(R.id.navAiChat).setOnClickListener {
            if (this !is AiChatActivity) {
                val intent = Intent(this, AiChatActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        findViewById<LinearLayout>(R.id.navSchedule).setOnClickListener {
            if (this !is FollowUpActivity) {
                val intent = Intent(this, FollowUpActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            if (this !is ProfileActivity) {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }
    }
}
