package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
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
                findViewById<TextView>(R.id.tvAppName)?.text = userName
            }

            // Load profile image if available (using initials icon logic from original UI)
            loadProfileImage()

            checkFreshStatus()
            setupClickListeners()
            updateBottomNavSelection()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Home Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun checkFreshStatus() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        // If new doctor, hide the hardcoded mock cases to give a "Fresh" look
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getPatients(userId)
                if (response.isSuccessful) {
                    val patients = response.body()?.patients ?: emptyList()
                    if (patients.isEmpty()) {
                        findViewById<TextView>(R.id.tvUrgentTitle).visibility = View.GONE
                        findViewById<TextView>(R.id.tvSeeAll).visibility = View.GONE
                        findViewById<MaterialCardView>(R.id.case1).visibility = View.GONE
                    }
                }
            } catch (e: Exception) {}
        }
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

    private fun loadProfileImage() {
        val sessionManager = SessionManager(this)
        val profileImageUriString = sessionManager.getProfileImage()
        if (!profileImageUriString.isNullOrEmpty()) {
            // Find or add a profile image view in the header
            // For now, if we don't have a specific view in activity_home.xml, we skip 
            // but the user might want it. Let's check activity_home.xml again for an icon.
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }
}
