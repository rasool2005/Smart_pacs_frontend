package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val sessionManager = SessionManager(this)
        val userName = sessionManager.getUserName()
        if (!userName.isNullOrEmpty()) {
            findViewById<TextView>(R.id.tvAppName).text = userName
        }

        setupClickListeners()
        updateBottomNavSelection()
    }

    private fun setupClickListeners() {

        findViewById<LinearLayout>(R.id.btnStudies).setOnClickListener {
            startActivity(Intent(this, StudiesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnReports).setOnClickListener {
            startActivity(Intent(this, ReportsListActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnAiChatQuick).setOnClickListener {
            startActivity(Intent(this, AiChatActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnScanNow).setOnClickListener {
            startActivity(Intent(this, AiScannerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.notificationCard).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.case1).setOnClickListener {
            val intent = Intent(this, AiResultsActivity::class.java)
            val samplePrediction = PredictionResponse(
                status = "success",
                scan_type = "CT Chest",
                confidence_score = 95.0,
                confidence_level = "High",
                message = "Analysis completed. Potential pulmonary nodule detected.",
                findings = listOf(
                    AiFinding(
                        title = "Pulmonary Nodule",
                        location = "Right Upper Lobe",
                        description = "A 6mm indeterminate solid nodule is noted. Recommend follow-up CT in 6 months.",
                        confidence = 95.0,
                        severity = "High"
                    )
                )
            )
            intent.putExtra("prediction_results", samplePrediction)
            // Use mock image or actual image resource if available
            intent.putExtra("image_res_id", R.drawable.img_mock_ct) 
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tvSeeAll).setOnClickListener {
            startActivity(Intent(this, PatientsActivity::class.java))
        }

        // Custom Bottom Navigation
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.navPatients).setOnClickListener {
            startActivity(Intent(this, PatientsActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navAiChat).setOnClickListener {
            startActivity(Intent(this, AiChatActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, FollowUpActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun updateBottomNavSelection() {
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        // Set Home as selected
        findViewById<ImageView>(R.id.ivHome).setColorFilter(brandBlue)
        findViewById<TextView>(R.id.tvHome).setTextColor(brandBlue)

        // Ensure others are unselected
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients).setTextColor(unselectedColor)

        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(unselectedColor)

        findViewById<ImageView>(R.id.ivProfile).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile).setTextColor(unselectedColor)
        
        val pink = ContextCompat.getColor(this, R.color.nav_ai_chat_pink)
        findViewById<TextView>(R.id.tvAiChatLabel).setTextColor(pink)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }
}
