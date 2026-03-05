package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StudiesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_studies)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Initialize bottom navigation
        setupBottomNavigation()

        // Back button logic
        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Helper to launch details
        fun openDetails(title: String, patient: String, date: String, criticality: String) {
            val intent = Intent(this, StudyDetailsActivity::class.java)
            intent.putExtra("STUDY_TITLE", title)
            intent.putExtra("PATIENT_INFO", patient)
            intent.putExtra("STUDY_DATE", date)
            intent.putExtra("CRITICALITY", criticality)
            startActivity(intent)
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardCtChest).setOnClickListener {
            openDetails("CT Chest", "John Doe • MRN-12345", "Jan 20, 2026", "High")
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMriBrain).setOnClickListener {
            openDetails("MRI Brain", "Sarah Smith • MRN-12346", "Jan 19, 2026", "Critical")
        }
        
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardXrayChest).setOnClickListener {
            openDetails("X-Ray Chest", "Mike Johnson • MRN-12347", "Jan 18, 2026", "Normal")
        }
        
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardCtAbdomen).setOnClickListener {
            openDetails("CT Abdomen", "Emily Davis • MRN-12348", "Jan 17, 2026", "High")
        }
    }
}
