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

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            val ivProfile = findViewById<ImageView>(R.id.ivProfileImage)
            ivProfile.setImageURI(resultUri)
            ivProfile.visibility = android.view.View.VISIBLE
            findViewById<TextView>(R.id.tvInitials).visibility = android.view.View.GONE
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "Crop error: ${UCrop.getError(result.data!!)?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            startCrop(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
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
        
        // Use the first letter of the doctor's name for initials
        val tvInitials = findViewById<TextView>(R.id.tvInitials)
        if (userName.isNotBlank()) {
            // If the name starts with "Dr. ", we might want the next letter, 
            // but the user said "first letter", so let's stick to that or check if it's "Dr."
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
        
        setupClickListeners()
        setupBottomNavigation()
        updateBottomNavSelection()
        fetchStats()
    }

    private fun fetchStats() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        lifecycleScope.launch {
            try {
                // Fetch AI Reports for cases count
                val reportsResponse = ApiClient.apiService.getAiReports(userId)
                if (reportsResponse.isSuccessful && reportsResponse.body()?.status == "success") {
                    val reports = reportsResponse.body()?.reports ?: emptyList()
                    val totalReports = reports.size
                    
                    // Count reports for current month
                    val currentMonthReports = countReportsInCurrentMonth(reports)
                    
                    findViewById<TextView>(R.id.tvCasesReviewed).text = totalReports.toString()
                    findViewById<TextView>(R.id.tvPatientsThisMonth).text = currentMonthReports.toString()
                }

                // Fetch Patients for AI Assists/Total Patients count
                val patientsResponse = ApiClient.apiService.getPatients(userId)
                if (patientsResponse.isSuccessful && patientsResponse.body()?.status == "success") {
                    val totalPatients = patientsResponse.body()?.patients?.size ?: 0
                    // We can display total patients or calculate a percentage
                    // For now, let's update the AI Assists stat with a count or percentage if desired
                    // The user mentioned "how many patient added", so maybe we use this for one of the stats
                    // Let's put total patients in the AI Assists slot if that's what they meant, 
                    // or keep it as a percentage and just update the others.
                    // Given the screenshot has "98% AI Assists", maybe it's (Reports/Patients)*100
                    
                    if (totalPatients > 0) {
                        val casesCount = findViewById<TextView>(R.id.tvCasesReviewed).text.toString().toIntOrNull() ?: 0
                        val percentage = if (totalPatients > 0) (casesCount.toDouble() / totalPatients * 100).toInt() else 0
                        findViewById<TextView>(R.id.tvAiAssists).text = "${minOf(percentage, 100)}%"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun countReportsInCurrentMonth(reports: List<AiReport>): Int {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        var count = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Adjust format based on API response
        
        for (report in reports) {
            try {
                // Backend usually sends ISO date like "2023-10-27T..." or "2023-10-27"
                val dateStr = report.created_at.split("T")[0]
                val date = sdf.parse(dateStr)
                if (date != null) {
                    calendar.time = date
                    if (calendar.get(Calendar.MONTH) == currentMonth && 
                        calendar.get(Calendar.YEAR) == currentYear) {
                        count++
                    }
                }
            } catch (e: Exception) {
                // Fallback or ignore malformed dates
            }
        }
        return count
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialCardView>(R.id.btnCamera).setOnClickListener {
            showImageSourceDialog()
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

        // Set Profile as selected
        findViewById<ImageView>(R.id.ivProfile).setColorFilter(brandBlue)
        findViewById<TextView>(R.id.tvProfile).setTextColor(brandBlue)

        // Ensure others are unselected
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
        fetchStats()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Image")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_profile.jpg"))
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.withAspectRatio(1f, 1f)
        
        val intent = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .getIntent(this)
            
        cropLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                val uri: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                photoUri = uri
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                cameraLauncher.launch(intent)
            }
        } else {
             Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
}
