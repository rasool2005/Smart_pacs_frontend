package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StudySummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_study_summary)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get data from intent
        val studyType = intent.getStringExtra("STUDY_TYPE") ?: "CT Chest"
        val studyDate = intent.getStringExtra("STUDY_DATE") ?: "Jan 20, 2026"

        findViewById<TextView>(R.id.tvStudyType).text = studyType
        findViewById<TextView>(R.id.tvStudyDate).text = studyDate

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Navigate to Viewer
        findViewById<Button>(R.id.btnViewImages).setOnClickListener {
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra("STUDY_TYPE", studyType)
            startActivity(intent)
        }

        // Navigate to AI Results
        findViewById<Button>(R.id.btnAiResults).setOnClickListener {
            val intent = Intent(this, AiResultsActivity::class.java)
            
            val formattedScanType = when {
                studyType.contains("CT", ignoreCase = true) -> "CT Scan"
                studyType.contains("MRI", ignoreCase = true) -> "MRI"
                else -> "X-Ray"
            }
            
            val imageRes = when (formattedScanType) {
                "CT Scan" -> R.drawable.real_ct_scan
                "MRI" -> R.drawable.real_mri_scan
                else -> R.drawable.real_xray_chest
            }
            
            intent.putExtra("scan_type", formattedScanType)
            intent.putExtra("image_res_id", imageRes)
            startActivity(intent)
        }
    }
}
