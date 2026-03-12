package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

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
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
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

        val cardCtChest = findViewById<MaterialCardView>(R.id.cardCtChest)
        cardCtChest.setOnClickListener {
            openDetails("CT Chest", "John Doe • MRN-12345", "Jan 20, 2026", "High")
        }

        val cardMriBrain = findViewById<MaterialCardView>(R.id.cardMriBrain)
        cardMriBrain.setOnClickListener {
            openDetails("MRI Brain", "Sarah Smith • MRN-12346", "Jan 19, 2026", "Critical")
        }
        
        val cardXrayChest = findViewById<MaterialCardView>(R.id.cardXrayChest)
        cardXrayChest.setOnClickListener {
            openDetails("X-Ray Chest", "Mike Johnson • MRN-12347", "Jan 18, 2026", "Normal")
        }
        
        val cardCtAbdomen = findViewById<MaterialCardView>(R.id.cardCtAbdomen)
        cardCtAbdomen.setOnClickListener {
            openDetails("CT Abdomen", "Emily Davis • MRN-12348", "Jan 17, 2026", "High")
        }

        // Delete button listeners
        findViewById<ImageView>(R.id.btnDeleteStudy1).setOnClickListener {
            showDeleteConfirmation(cardCtChest)
        }
        findViewById<ImageView>(R.id.btnDeleteStudy2).setOnClickListener {
            showDeleteConfirmation(cardMriBrain)
        }
        findViewById<ImageView>(R.id.btnDeleteStudy3).setOnClickListener {
            showDeleteConfirmation(cardXrayChest)
        }
        findViewById<ImageView>(R.id.btnDeleteStudy4).setOnClickListener {
            showDeleteConfirmation(cardCtAbdomen)
        }
    }

    private fun showDeleteConfirmation(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Delete Study")
            .setMessage("Are you sure you want to delete this study?")
            .setPositiveButton("Delete") { _, _ ->
                view.visibility = View.GONE
                Toast.makeText(this, "Study deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
