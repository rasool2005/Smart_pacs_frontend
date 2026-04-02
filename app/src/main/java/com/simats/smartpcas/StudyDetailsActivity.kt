package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class StudyDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_study_details)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val title = intent.getStringExtra("STUDY_TITLE") ?: "CT Chest"
        val date = intent.getStringExtra("STUDY_DATE") ?: "Jan 20, 2026"

        findViewById<TextView>(R.id.tvStudyType).text = title
        findViewById<TextView>(R.id.tvStudyDate).text = date
        findViewById<TextView>(R.id.tvStudyStatus).text = "Completed"
        
        findViewById<MaterialButton>(R.id.btnAiResults).text = "Scan Image for AI Analysis"
        findViewById<MaterialButton>(R.id.btnAiResults).setOnClickListener {
            val intent = Intent(this, AiScannerActivity::class.java)
            intent.putExtra("PATIENT_NAME", intent.getStringExtra("PATIENT_INFO"))
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnViewImages).setOnClickListener {
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra("STUDY_TYPE", title)
            startActivity(intent)
        }
    }
}
