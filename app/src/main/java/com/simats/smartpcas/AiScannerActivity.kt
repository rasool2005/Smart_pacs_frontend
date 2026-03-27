package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AiScannerActivity : BaseActivity() {

    private var selectedScanType = "X-Ray"

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImageForPrediction(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_scanner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        findViewById<MaterialButton>(R.id.btnPACS).setOnClickListener {
            val intent = Intent(this, StudiesActivity::class.java)
            startActivity(intent)
        }

        setupBottomNavigation()
        setupScannerTabs()
    }

    private fun setupScannerTabs() {
        val tvTabXray = findViewById<TextView>(R.id.tvTabXray)
        val tvTabCt = findViewById<TextView>(R.id.tvTabCt)
        val tvTabMri = findViewById<TextView>(R.id.tvTabMri)
        val tvInstruction = findViewById<TextView>(R.id.tvInstruction)

        tvTabXray.setOnClickListener {
            updateTabs("X-Ray", tvTabXray, tvTabCt, tvTabMri, tvInstruction)
        }
        tvTabCt.setOnClickListener {
            updateTabs("CT Scan", tvTabCt, tvTabXray, tvTabMri, tvInstruction)
        }
        tvTabMri.setOnClickListener {
            updateTabs("MRI", tvTabMri, tvTabXray, tvTabCt, tvInstruction)
        }
    }

    private fun updateTabs(type: String, activeTab: TextView, inactive1: TextView, inactive2: TextView, instruction: TextView) {
        selectedScanType = type
        
        activeTab.setBackgroundResource(R.drawable.bg_white_rounded)
        activeTab.setTextColor(ContextCompat.getColor(this, R.color.purple_gradient_start))
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD)

        val inactiveBg = R.drawable.bg_white_alpha_rounded
        inactive1.setBackgroundResource(inactiveBg)
        inactive1.setTextColor(ContextCompat.getColor(this, R.color.white))
        inactive1.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        inactive2.setBackgroundResource(inactiveBg)
        inactive2.setTextColor(ContextCompat.getColor(this, R.color.white))
        inactive2.setTypeface(null, android.graphics.Typeface.NORMAL)

        instruction.text = "Upload ${type.uppercase()} image for instant AI\ndiagnosis"
    }

    // ✅ Multipart image upload logic for real device
    private fun uploadImageForPrediction(uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        
        // Use the internal file URI to avoid SecurityException
        val internalUri = Uri.fromFile(file)

        // 🚨 Validate if image is a medical scan
        if (!isMedicalImage(file)) {
            Toast.makeText(this, "Invalid Image: Please upload only X-Ray, CT, or MRI medical scans.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Create RequestBody for file
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        // 2. Create MultipartBody.Part (key is 'file' to match backend)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // 3. Create RequestBody for scan_type
        val scanTypeBody = selectedScanType.toRequestBody("text/plain".toMediaTypeOrNull())

        Toast.makeText(this, "Processing $selectedScanType...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.predictImage(body, scanTypeBody)

                if (response.isSuccessful && response.body() != null) {
                    val prediction = response.body()!!
                    val intent = Intent(this@AiScannerActivity, AiResultsActivity::class.java)
                    intent.putExtra("image_uri", internalUri.toString())
                    intent.putExtra("prediction_results", prediction)
                    intent.putExtra("scan_type", selectedScanType)
                    intent.putExtra("PATIENT_NAME", this@AiScannerActivity.intent.getStringExtra("PATIENT_NAME"))
                    startActivity(intent)
                } else {
                    // Pass internal URI and scan type to AiResultsActivity even on failure
                    val intent = Intent(this@AiScannerActivity, AiResultsActivity::class.java)
                    intent.putExtra("image_uri", internalUri.toString())
                    intent.putExtra("scan_type", selectedScanType)
                    intent.putExtra("PATIENT_NAME", this@AiScannerActivity.intent.getStringExtra("PATIENT_NAME"))
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // Connection error fallback
                val intent = Intent(this@AiScannerActivity, AiResultsActivity::class.java)
                intent.putExtra("image_uri", internalUri.toString())
                intent.putExtra("scan_type", selectedScanType)
                intent.putExtra("PATIENT_NAME", this@AiScannerActivity.intent.getStringExtra("PATIENT_NAME"))
                startActivity(intent)
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            // Use a unique name to prevent overwriting images in history
            val fileName = "scan_${UUID.randomUUID()}.jpg"
            val file = File(filesDir, fileName) // Use filesDir for better persistence than cacheDir
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stricter Medical Image Heuristic:
     * - Checks for colorful photos (most pixels > 25 variance)
     * - Checks for documents/screenshots (large amounts of pure white background)
     * - Checks for medical backgrounds (must have some dark/black unexposed film)
     */
    private fun isMedicalImage(file: File): Boolean {
        try {
            val options = android.graphics.BitmapFactory.Options()
            options.inSampleSize = 4 // Scale down to process faster
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, options) ?: return true
            
            val width = bitmap.width
            val height = bitmap.height
            if (width * height == 0) return true
            
            var coloredPixels = 0
            var darkPixels = 0
            var whitePixels = 0
            var checkedPixels = 0
            
            // Sample a subset of pixels to be fast (check every 3rd pixel)
            for (y in 0 until height step 3) {
                for (x in 0 until width step 3) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    
                    val maxColor = maxOf(r, g, b)
                    val minColor = minOf(r, g, b)
                    val variance = maxColor - minColor
                    
                    if (variance > 25) {
                        coloredPixels++
                    }
                    if (maxColor < 40) {
                        darkPixels++
                    }
                    if (minColor > 240) {
                        whitePixels++
                    }
                    
                    checkedPixels++
                }
            }
            
            bitmap.recycle() // Free memory
            
            if (checkedPixels == 0) return true
            
            val coloredPercentage = (coloredPixels.toFloat() / checkedPixels) * 100f
            val darkPercentage = (darkPixels.toFloat() / checkedPixels) * 100f
            val whitePercentage = (whitePixels.toFloat() / checkedPixels) * 100f
            
            // 1. Reject colorful photos (more than 5% of pixels have a distinct hue)
            if (coloredPercentage > 5.0f) return false
            
            // 2. Reject screenshots/documents (more than 60% of the image is pure bright white)
            if (whitePercentage > 60.0f) return false
            
            // 3. Reject images with no dark background (X-Rays/CTs almost always have some dark background)
            if (darkPercentage < 5.0f) return false
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fallback to allow if error parsing
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        val pink = ContextCompat.getColor(this, R.color.nav_ai_chat_pink)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        findViewById<ImageView>(R.id.ivHome).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome).setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients).setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivProfile).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile).setTextColor(unselectedColor)

        findViewById<TextView>(R.id.tvAiChatLabel).setTextColor(pink)
    }
}
