package com.simats.smartpcas

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class SecurityActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_security)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Toggles
        findViewById<SwitchMaterial>(R.id.switch2FA).setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "2FA Enabled" else "2FA Disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        findViewById<SwitchMaterial>(R.id.switchBiometric).setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Biometric Auth Enabled" else "Biometric Auth Disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Sign Out Buttons
        findViewById<TextView>(R.id.btnSignOut1).setOnClickListener {
            Toast.makeText(this, "Signed out from iPad Air", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnSignOut2).setOnClickListener {
            Toast.makeText(this, "Signed out from MacBook Pro", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnSignOutAll).setOnClickListener {
            Toast.makeText(this, "Signed out from all other devices", Toast.LENGTH_SHORT).show()
        }

        // Login Activity
        findViewById<android.view.View>(R.id.cardLoginActivity).setOnClickListener {
            startActivity(android.content.Intent(this, LoginHistoryActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
