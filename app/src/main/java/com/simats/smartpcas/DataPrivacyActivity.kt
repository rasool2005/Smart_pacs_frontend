package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DataPrivacyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_data_privacy)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back Button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Simulated Data Actions
        findViewById<View>(R.id.btnDownloadData).setOnClickListener {
            Toast.makeText(this, "Requesting data archive...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnDataInfo).setOnClickListener {
            Toast.makeText(this, "Opening Data Practices info...", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.btnPrivacyPolicy).setOnClickListener {
            Toast.makeText(this, "Opening Privacy Policy...", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.btnTerms).setOnClickListener {
            Toast.makeText(this, "Opening Terms of Service...", Toast.LENGTH_SHORT).show()
        }

        // Delete Account
        findViewById<View>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone and all your data will be lost.")
            .setPositiveButton("Delete") { _, _ ->
                // Simulate Deletion
                Toast.makeText(this, "Account Deleted", Toast.LENGTH_LONG).show()
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        SessionManager(this).logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
