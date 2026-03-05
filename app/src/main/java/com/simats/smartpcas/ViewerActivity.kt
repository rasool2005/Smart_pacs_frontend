package com.simats.smartpcas

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_viewer)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val studyType = intent.getStringExtra("STUDY_TYPE") ?: "CT Chest"
        
        val titleText = when {
            studyType.contains("MRI", ignoreCase = true) -> "$studyType - T2 Weighted"
            studyType.contains("CT", ignoreCase = true) -> "$studyType - Axial View"
            studyType.contains("X-Ray", ignoreCase = true) -> "$studyType - PA View"
            else -> "$studyType - View"
        }

        findViewById<TextView>(R.id.tvViewerTitle).text = titleText
        
        val ivMainScan = findViewById<ImageView>(R.id.ivMainScan)
        
        // Ensure that any MRI title loads the realistic image
        val imageRes = if (studyType.contains("MRI", ignoreCase = true)) {
            R.drawable.real_mri_scan
        } else if (studyType.contains("CT", ignoreCase = true)) {
            R.drawable.real_ct_scan
        } else if (studyType.contains("X-Ray", ignoreCase = true)) {
            R.drawable.real_xray_chest
        } else {
            R.drawable.img_mock_mri
        }

        ivMainScan.setImageResource(imageRes)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
