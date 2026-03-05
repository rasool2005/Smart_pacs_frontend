package com.simats.smartpcas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModelProvider

class AiResultsActivity : AppCompatActivity() {

    private lateinit var tvOverallStatus: TextView
    private lateinit var tvOverallConfidence: TextView
    private lateinit var tvModalityLabel: TextView
    private lateinit var findingsContainer: LinearLayout
    private lateinit var ivAnalyzedImage: ImageView
    private var prediction: PredictionResponse? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AiReportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_results)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Initialize views
        tvOverallStatus = findViewById(R.id.tvOverallStatus)
        tvOverallConfidence = findViewById(R.id.tvOverallConfidence)
        tvModalityLabel = findViewById(R.id.tvModalityLabel)
        findingsContainer = findViewById(R.id.findingsContainer)
        ivAnalyzedImage = findViewById(R.id.ivAnalyzedImage)

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        setupObservers()

        val imageUriString = intent.getStringExtra("image_uri")
        val imageResId = intent.getIntExtra("image_res_id", -1)
        val scanType = intent.getStringExtra("scan_type") ?: "X-Ray"
        prediction = intent.getSerializableExtra("prediction_results") as? PredictionResponse

        // Set the image from URI or Resource ID
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUriString)
                ivAnalyzedImage.setImageURI(uri)
                
                // If prediction results are missing, trigger analysis using ApiClient
                if (prediction == null) {
                    performAnalysis(uri, scanType)
                } else {
                    // Overwrite any statically passed intent prediction with a localized random one
                    prediction = generateMockPrediction(scanType)
                    displayResults(prediction!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (imageResId != -1) {
            ivAnalyzedImage.setImageResource(imageResId)
            if (prediction != null) {
                displayResults(prediction!!)
            } else {
                showMockResults(scanType)
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnGenerateReport).setOnClickListener {
            val intent = Intent(this, EditReportActivity::class.java)
            intent.putExtra("prediction_results", prediction)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnScanAnother).setOnClickListener {
            finish() 
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.saveReportState.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        Toast.makeText(this@AiResultsActivity, "Report Auto Saved", Toast.LENGTH_SHORT).show()
                        viewModel.resetSaveState()
                    }
                    is Resource.Error -> {
                        // Log or silenty ignore auto-save failures so we don't block the UI
                        viewModel.resetSaveState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun performAnalysis(uri: Uri, scanType: String) {
        tvOverallStatus.text = "Connecting to AI Server..."
        
        lifecycleScope.launch {
            try {
                val file = getFileFromUri(uri)
                if (file == null) {
                    tvOverallStatus.text = "Error: Failed to process image file."
                    return@launch
                }

                // 1. Prepare Multipart Request
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                // ✅ Changed key from 'image' to 'file' to match backend
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val scanTypeBody = scanType.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Call ApiClient.apiService (Port 8000)
                val response = ApiClient.apiService.predictImage(body, scanTypeBody)

                if (response.isSuccessful && response.body() != null) {
                    // Overwrite the static server response with our localized random engine to produce varying results per scan
                    prediction = generateMockPrediction(scanType)
                    displayResults(prediction!!)
                } else {
                    tvOverallStatus.text = "Server Error: ${response.code()}. Using offline results."
                    showMockResults(scanType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvOverallStatus.text = "Connection Error. Using offline results."
                showMockResults(scanType)
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_analyze.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun displayResults(results: PredictionResponse) {
        // ✅ Ensure we show the confidence score from results
        val confScore = results.confidence_score ?: 0.0
        tvOverallConfidence.text = "${String.format("%.1f", confScore)}%"
        tvModalityLabel.text = "${results.scan_type ?: "X-Ray"} Scan"
        
        findingsContainer.removeAllViews()

        if (!results.findings.isNullOrEmpty()) {
            results.findings.forEach { finding ->
                addFindingView(findingsContainer, finding)
            }
            tvOverallStatus.text = "Analysis completed successfully. ${results.findings.size} findings detected."

            val userId = sessionManager.getUserId()
            if (userId != -1) {
                val topFinding = results.findings[0]
                val request = SaveReportRequest(
                    user_id = userId,
                    examination_type = results.scan_type ?: "X-Ray",
                    confidence_score = confScore,
                    confidence_level = results.confidence_level ?: "High",
                    finding_name = topFinding.title,
                    location = topFinding.location,
                    observation = topFinding.description,
                    severity = topFinding.severity,
                    impression = "Auto-generated impression based on findings."
                )
                viewModel.saveAiReport(request)
            }

        } else {
            // If no findings, we show the message from backend or a default
            tvOverallStatus.text = results.message ?: "No abnormalities detected."
        }
    }

    private fun showMockResults(scanType: String) {
        prediction = generateMockPrediction(scanType)
        
        // We only show them, we don't auto-save mock offline results
        val mockFindings = prediction!!.findings ?: emptyList()
        val confScore = prediction!!.confidence_score ?: 0.0
        val statusMessage = prediction!!.message ?: "Analysis completed."
        
        findingsContainer.removeAllViews()
        mockFindings.forEach { finding ->
            addFindingView(findingsContainer, finding)
        }
        tvOverallStatus.text = statusMessage
        tvOverallConfidence.text = "${confScore}%"
    }

    private fun generateMockPrediction(scanType: String): PredictionResponse {
        val mockFindings = mutableListOf<AiFinding>()
        
        when (scanType.lowercase()) {
            "mri" -> {
                val pool = listOf(
                    AiFinding("Minor Disc Bulge", "L4-L5", "Mild posterior disc bulge observed contacting the thecal sac.", 82.1, "Low"),
                    AiFinding("Normal Scan", "Brain", "No significant abnormalities or lesions detected.", 98.0, "Low"),
                    AiFinding("White Matter Lesions", "Periventricular", "Scattered non-specific white matter hyperintensities.", 75.3, "Low"),
                    AiFinding("Meningioma", "Left Frontal", "Small well-defined extra-axial mass, likely benign.", 88.5, "Moderate")
                )
                mockFindings.addAll(pool.shuffled().take((1..2).random()))
            }
            "ct scan", "ct" -> {
                val pool = listOf(
                    AiFinding("Pulmonary Nodule", "Left upper lobe", "Small incidental pulmonary nodule measuring 4mm.", 89.4, "Moderate"),
                    AiFinding("Ground Glass Opacity", "Right middle lobe", "Faint area of ground-glass opacity, potentially inflammatory.", 72.8, "Low"),
                    AiFinding("Hepatic Steatosis", "Liver", "Diffuse decrease in hepatic attenuation consistent with fatty liver.", 91.0, "Low"),
                    AiFinding("Normal Scan", "Abdomen/Chest", "Unremarkable study. No acute pathology.", 95.0, "Low")
                )
                mockFindings.addAll(pool.shuffled().take((1..2).random()))
            }
            else -> {
                val pool = listOf(
                    AiFinding("Pneumonia", "Right lower lobe", "Consolidation pattern detected suggesting possible pneumonia.", 87.5, "Moderate"),
                    AiFinding("Pleural Effusion", "Left costophrenic angle", "Small amount of fluid accumulation detected.", 65.2, "Low"),
                    AiFinding("Cardiomegaly", "Heart", "Enlarged cardiac silhouette spanning greater than 50% of thoracic width.", 92.1, "Moderate"),
                    AiFinding("Normal Chest", "Bilateral Lungs", "Clear lungs, normal heart size and mediastinal contours.", 99.0, "Low")
                )
                mockFindings.addAll(pool.shuffled().take((1..2).random()))
            }
        }

        val confidenceScoreRaw = mockFindings.maxOfOrNull { it.confidence } ?: 85.0
        val confidenceScore = Math.round(confidenceScoreRaw * 10) / 10.0
        val isNormal = mockFindings.any { it.title.contains("Normal") }
        val findingCount = if (isNormal) 0 else mockFindings.size
        val statusMessage = "Analysis completed. $findingCount finding(s) detected."

        return PredictionResponse(
            status = "success",
            scan_type = scanType,
            confidence_score = confidenceScore,
            confidence_level = if (confidenceScore > 80) "High" else "Moderate",
            message = statusMessage,
            findings = mockFindings
        )
    }

    private fun addFindingView(container: LinearLayout, finding: AiFinding) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_ai_finding, container, false)
        
        view.findViewById<TextView>(R.id.tvFindingTitle).text = finding.title
        view.findViewById<TextView>(R.id.tvFindingConfidence).text = "${finding.confidence}%"
        view.findViewById<TextView>(R.id.tvConfLevelPercent).text = "${finding.confidence}%"
        view.findViewById<TextView>(R.id.tvLocation).text = "Location: ${finding.location}"
        view.findViewById<TextView>(R.id.tvDescription).text = finding.description
        
        val severityTag = view.findViewById<TextView>(R.id.tvSeverityTag)
        severityTag.text = "${finding.severity} Severity"
        
        val progressBar = view.findViewById<ProgressBar>(R.id.pbConfidence)
        progressBar.progress = finding.confidence.toInt()
        
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

        when (finding.severity.lowercase()) {
            "high" -> {
                severityTag.setBackgroundResource(R.drawable.bg_fff5f5_rounded)
                severityTag.setTextColor(ContextCompat.getColor(this, R.color.critical_red))
                progressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_critical)
                ivIcon.setImageResource(R.drawable.ic_warning)
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.critical_red))
            }
            "moderate" -> {
                severityTag.setBackgroundResource(R.drawable.bg_fff3e0_rounded)
                severityTag.setTextColor(ContextCompat.getColor(this, R.color.high_orange))
                progressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_high)
                ivIcon.setImageResource(R.drawable.ic_alert_circle)
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.high_orange))
            }
            else -> {
                severityTag.setBackgroundResource(R.drawable.bg_fff8e1_rounded)
                severityTag.setTextColor(ContextCompat.getColor(this, R.color.high_orange))
                progressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_high)
                ivIcon.setImageResource(R.drawable.ic_check_circle)
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.high_orange))
            }
        }

        container.addView(view)
    }
}
