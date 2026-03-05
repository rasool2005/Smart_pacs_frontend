package com.simats.smartpcas

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Legal Links
        findViewById<View>(R.id.btnLegalTerms).setOnClickListener {
            startActivity(android.content.Intent(this, TermsActivity::class.java))
        }

        findViewById<View>(R.id.btnLegalPrivacy).setOnClickListener {
            startActivity(android.content.Intent(this, DataPrivacyActivity::class.java))
        }
        
        findViewById<View>(R.id.btnLegalLicense).setOnClickListener {
            android.widget.Toast.makeText(this, "Opening License Agreement...", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.btnLegalOpenSource).setOnClickListener {
            android.widget.Toast.makeText(this, "Opening Open Source Licenses...", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
