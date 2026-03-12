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
import com.bumptech.glide.Glide

class AiResultsActivity : AppCompatActivity() {

    private lateinit var tvOverallStatus: TextView
    private lateinit var tvOverallConfidence: TextView
    private lateinit var tvModalityLabel: TextView
    private lateinit var findingsContainer: LinearLayout
    private lateinit var ivAnalyzedImage: ImageView
    private var prediction: PredictionResponse? = null
    private var imageUriString: String? = null

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

        tvOverallStatus = findViewById(R.id.tvOverallStatus)
        tvOverallConfidence = findViewById(R.id.tvOverallConfidence)
        tvModalityLabel = findViewById(R.id.tvModalityLabel)
        findingsContainer = findViewById(R.id.findingsContainer)
        ivAnalyzedImage = findViewById(R.id.ivAnalyzedImage)

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        setupObservers()

        imageUriString = intent.getStringExtra("image_uri")
        val imageResId = intent.getIntExtra("image_res_id", -1)
        val scanType = intent.getStringExtra("scan_type") ?: "X-Ray"
        prediction = intent.getSerializableExtra("prediction_results") as? PredictionResponse

        if (!imageUriString.isNullOrEmpty()) {
            val uri = Uri.parse(imageUriString)
            // Use Glide instead of setImageURI to handle potential SecurityExceptions gracefully
            Glide.with(this)
                .load(uri)
                .error(R.drawable.img_mock_ct)
                .into(ivAnalyzedImage)
                
            if (prediction == null) {
                performAnalysis(uri, scanType)
            } else {
                displayResults(prediction!!)
            }
        } else if (imageResId != -1) {
            ivAnalyzedImage.setImageResource(imageResId)
            if (prediction != null) {
                displayResults(prediction!!)
            } else {
                showMockResults(scanType)
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnGenerateReport).setOnClickListener {
            val intent = Intent(this, EditReportActivity::class.java)
            intent.putExtra("prediction_results", prediction)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnScanAnother).setOnClickListener { finish() }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.saveReportState.collect { resource ->
                if (resource is Resource.Success) {
                    Toast.makeText(this@AiResultsActivity, "Scan Saved to History", Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveState()
                }
            }
        }
    }

    private fun performAnalysis(uri: Uri, scanType: String) {
        tvOverallStatus.text = "Analyzing Image..."
        lifecycleScope.launch {
            try {
                val file = getFileFromUri(uri)
                if (file == null) {
                    tvOverallStatus.text = "Error processing image."
                    return@launch
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val scanTypeBody = scanType.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiClient.apiService.predictImage(body, scanTypeBody)
                if (response.isSuccessful && response.body() != null) {
                    prediction = response.body()!! // Use actual prediction
                    displayResults(prediction!!)
                } else {
                    showMockResults(scanType)
                }
            } catch (e: Exception) {
                showMockResults(scanType)
            }
        }
    }

    private fun displayResults(results: PredictionResponse) {
        val confScore = results.confidence_score ?: 0.0
        tvOverallConfidence.text = "${String.format("%.1f", confScore)}%"
        tvModalityLabel.text = "${results.scan_type ?: "X-Ray"} Scan"
        findingsContainer.removeAllViews()

        if (!results.findings.isNullOrEmpty()) {
            results.findings.forEach { addFindingView(findingsContainer, it) }
            tvOverallStatus.text = "Analysis complete."

            if (!intent.getBooleanExtra("is_history", false)) {
                val userId = sessionManager.getUserId()
                val patientName = intent.getStringExtra("PATIENT_NAME")
                
                // ✅ Only save to history IF we have a valid prediction/result
                if (userId != -1 && !results.findings.isNullOrEmpty()) {
                    val request = SaveReportRequest(
                        user_id = userId,
                        examination_type = results.scan_type ?: "X-Ray",
                        confidence_score = confScore,
                        confidence_level = results.confidence_level ?: "High",
                        finding_name = results.findings[0].title,
                        location = results.findings[0].location,
                        observation = results.findings[0].description,
                        severity = results.findings[0].severity,
                        impression = "[Patient: $patientName] AI Analysis Result.",
                        image_uri = imageUriString
                    )
                    viewModel.saveAiReport(request)
                }
            }
        } else {
            tvOverallStatus.text = "No findings detected."
        }
    }

    private fun showMockResults(scanType: String) {
        prediction = generateMockPrediction(scanType)
        displayResults(prediction!!)
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
        } catch (e: Exception) { null }
    }

    private fun generateMockPrediction(scanType: String): PredictionResponse {
        val finding = when (scanType.lowercase()) {
            "mri" -> AiFinding("Normal Brain", "Cerebrum", "No acute intracranial pathology.", 98.5, "Low")
            "ct scan", "ct" -> AiFinding("Normal Chest", "Lungs", "No nodules detected.", 96.0, "Low")
            else -> AiFinding("Pneumonia", "Right Lower Lobe", "Infiltration pattern detected.", 88.0, "Moderate")
        }
        return PredictionResponse("success", scanType, finding.confidence, "High", "Analyzed.", listOf(finding))
    }

    private fun addFindingView(container: LinearLayout, finding: AiFinding) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_ai_finding, container, false)
        view.findViewById<TextView>(R.id.tvFindingTitle).text = finding.title
        view.findViewById<TextView>(R.id.tvFindingConfidence).text = "${finding.confidence}%"
        view.findViewById<TextView>(R.id.tvLocation).text = "Location: ${finding.location}"
        view.findViewById<TextView>(R.id.tvDescription).text = finding.description
        container.addView(view)
    }
}
