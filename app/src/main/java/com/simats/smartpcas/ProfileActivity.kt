package com.simats.smartpcas


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : BaseActivity() {

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            val ivProfile = findViewById<ImageView>(R.id.ivProfileImage)
            ivProfile.setImageURI(resultUri)
            ivProfile.visibility = android.view.View.VISIBLE
            findViewById<TextView>(R.id.tvInitials).visibility = android.view.View.GONE
            
            // ✅ Persist the profile image
            SessionManager(this).saveProfileImage(resultUri.toString())
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "Crop error: ${UCrop.getError(result.data!!)?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            startCrop(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val sessionManager = SessionManager(this)
        val userDetails = sessionManager.getUserDetails()
        val userName = userDetails[SessionManager.KEY_USER_NAME] ?: "User Name"
        
        findViewById<TextView>(R.id.tvDoctorName).text = userName
        
        val tvInitials = findViewById<TextView>(R.id.tvInitials)
        if (userName.isNotBlank()) {
            val cleanName = if (userName.startsWith("Dr. ", ignoreCase = true)) {
                userName.substring(4).trim()
            } else {
                userName.trim()
            }
            if (cleanName.isNotEmpty()) {
                tvInitials.text = cleanName[0].uppercaseChar().toString()
            } else {
                tvInitials.text = userName[0].uppercaseChar().toString()
            }
        }
        
        // ✅ Load existing profile image if available
        val profileImageUri = sessionManager.getProfileImage()
        if (!profileImageUri.isNullOrEmpty()) {
            val ivProfile = findViewById<ImageView>(R.id.ivProfileImage)
            ivProfile.setImageURI(Uri.parse(profileImageUri))
            ivProfile.visibility = android.view.View.VISIBLE
            tvInitials.visibility = android.view.View.GONE
        }
        
        setupClickListeners()
        setupBottomNavigation()
        updateBottomNavSelection()
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialCardView>(R.id.btnCamera).setOnClickListener {
            showImageOptionsDialog()
        }

        findViewById<RelativeLayout>(R.id.btnPersonalInfo).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        findViewById<RelativeLayout>(R.id.btnChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<RelativeLayout>(R.id.btnDataPrivacy).setOnClickListener {
            startActivity(Intent(this, DataPrivacyActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<RelativeLayout>(R.id.btnSecurity).setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<RelativeLayout>(R.id.btnHelpSupport).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<android.view.View>(R.id.btnLogout).setOnClickListener {
            SessionManager(this).logout()
            val intent = Intent(this, HospitalSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
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
            // Already here
        }
    }

    private fun updateBottomNavSelection() {
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        findViewById<ImageView>(R.id.ivProfile).setColorFilter(brandBlue)
        findViewById<TextView>(R.id.tvProfile).setTextColor(brandBlue)

        findViewById<ImageView>(R.id.ivHome).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(unselectedColor)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }

    private fun showImageOptionsDialog() {
        val options = arrayOf("Change Image", "Remove Image", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> removeProfileImage()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun removeProfileImage() {
        val sessionManager = SessionManager(this)
        sessionManager.saveProfileImage("") // Clear saved path
        
        findViewById<ImageView>(R.id.ivProfileImage).apply {
            setImageDrawable(null)
            visibility = View.GONE
        }
        findViewById<TextView>(R.id.tvInitials).visibility = View.VISIBLE
        
        Toast.makeText(this, "Profile image removed", Toast.LENGTH_SHORT).show()
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_profile.jpg"))
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.withAspectRatio(1f, 1f)
        
        // ✅ Fix: Use Black status bar and Brand Blue toolbar. 
        // Theme.UCrop with fitsSystemWindows will push the toolbar down.
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        options.setToolbarColor(brandBlue)
        options.setStatusBarColor(android.graphics.Color.BLACK)
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white))
        options.setToolbarTitle("Edit Profile Photo")
        
        val intent = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .getIntent(this)
            
        cropLauncher.launch(intent)
    }
}
