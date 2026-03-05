package com.simats.smartpcas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HelpSupportActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Quick Actions
        findViewById<android.view.View>(R.id.cardLiveChat).setOnClickListener {
             startActivity(Intent(this, AiChatActivity::class.java))
             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<android.view.View>(R.id.cardEmailUs).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@smartpacs.ai")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.view.View>(R.id.cardCallSupport).setOnClickListener {
             val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+18001234567")
            }
             try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No dialer app found", Toast.LENGTH_SHORT).show()
            }
        }

        // Submit Feedback
        findViewById<android.view.View>(R.id.btnSubmitFeedback).setOnClickListener {
             startActivity(Intent(this, FeedbackActivity::class.java))
             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Resources
        findViewById<TextView>(R.id.btnPrivacyPolicy).setOnClickListener {
             startActivity(Intent(this, DataPrivacyActivity::class.java))
             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<TextView>(R.id.btnTerms).setOnClickListener {
             startActivity(Intent(this, TermsActivity::class.java))
             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<TextView>(R.id.btnUserGuide).setOnClickListener {
            Toast.makeText(this, "Downloading PDF...", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<TextView>(R.id.btnVideoTutorials).setOnClickListener {
             Toast.makeText(this, "Opening Video Tutorials...", Toast.LENGTH_SHORT).show()
        }

        // FAQs
        setupFaqItem(R.id.faqItem1, R.id.faqAnswer1, R.id.faqChevron1)
        setupFaqItem(R.id.faqItem2, R.id.faqAnswer2, R.id.faqChevron2)
        setupFaqItem(R.id.faqItem3, R.id.faqAnswer3, R.id.faqChevron3)
    }

    private fun setupFaqItem(containerId: Int, answerId: Int, chevronId: Int) {
        val container = findViewById<android.view.View>(containerId)
        val answer = findViewById<android.view.View>(answerId)
        val chevron = findViewById<android.view.View>(chevronId)

        container.setOnClickListener {
            if (answer.visibility == android.view.View.VISIBLE) {
                answer.visibility = android.view.View.GONE
                chevron.animate().rotation(0f).setDuration(200).start()
            } else {
                answer.visibility = android.view.View.VISIBLE
                chevron.animate().rotation(90f).setDuration(200).start()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
