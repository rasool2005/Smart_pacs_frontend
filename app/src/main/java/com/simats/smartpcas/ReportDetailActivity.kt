package com.simats.smartpcas

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: AiReportViewModel
    private var report: AiReport? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnEmailReport: MaterialButton
    private lateinit var etPatientEmail: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        report = intent.getSerializableExtra("report_data") as? AiReport

        if (report == null) {
            Toast.makeText(this, "Error loading report details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        populateData(report!!)
        setupObservers()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnCancel.setOnClickListener {
            finish()
        }

        btnEmailReport.setOnClickListener {
            val email = etPatientEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val request = SendEmailRequest(report!!.id, email)
            viewModel.sendReportEmail(request)
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        btnCancel = findViewById(R.id.btnCancel)
        btnEmailReport = findViewById(R.id.btnEmailReport)
        etPatientEmail = findViewById(R.id.etPatientEmail)
    }

    private fun populateData(report: AiReport) {
        val ivReportImage = findViewById<ImageView>(R.id.ivReportImage)
        
        if (!report.image_uri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(report.image_uri)
                com.bumptech.glide.Glide.with(this)
                    .load(uri)
                    .error(R.drawable.img_mock_ct)
                    .into(ivReportImage)
            } catch (e: Exception) {
                loadFallbackImage(ivReportImage, report.examination_type)
            }
        } else {
            loadFallbackImage(ivReportImage, report.examination_type)
        }

        findViewById<TextView>(R.id.tvExaminationType).text = "${report.examination_type} Scan"
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = parser.parse(report.created_at)
            findViewById<TextView>(R.id.tvDate).text = if (date != null) formatter.format(date) else report.created_at
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvDate).text = report.created_at
        }
        
        findViewById<TextView>(R.id.tvFindingName).text = report.finding_name
        findViewById<TextView>(R.id.tvLocation).text = "Location: ${report.location}"
        findViewById<TextView>(R.id.tvObservation).text = report.observation
        findViewById<TextView>(R.id.tvConfidenceLevel).text = report.confidence_level
        findViewById<TextView>(R.id.tvConfidenceScore).text = "(${report.confidence_score}%)"
        findViewById<TextView>(R.id.tvImpression).text = report.impression
    }

    private fun loadFallbackImage(ivReportImage: ImageView, examinationType: String) {
        val imageResId = when (examinationType.lowercase()) {
            "ct scan", "ct" -> R.drawable.real_ct_scan
            "mri", "mri brain" -> R.drawable.real_mri
            "x-ray", "xray", "x-ray chest" -> R.drawable.real_xray_chest
            else -> R.drawable.img_mock_ct
        }
        ivReportImage.setImageResource(imageResId)
    }

    private fun setupObservers() {

        lifecycleScope.launch {
            viewModel.sendEmailState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnEmailReport.isEnabled = false
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        btnEmailReport.isEnabled = true
                        Toast.makeText(this@ReportDetailActivity, "Email sent successfully", Toast.LENGTH_SHORT).show()
                        etPatientEmail.text?.clear()
                        viewModel.resetEmailState()
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        btnEmailReport.isEnabled = true
                        Toast.makeText(this@ReportDetailActivity, resource.message, Toast.LENGTH_LONG).show()
                        viewModel.resetEmailState()
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun savePdfToDownloads(body: ResponseBody?) {
        if (body == null) {
            Toast.makeText(this, "Failed to download PDF: Empty response", Toast.LENGTH_SHORT).show()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val fileName = "AI_Report_${report?.id}_${System.currentTimeMillis()}.pdf"
                
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    var inputStream: InputStream? = null
                    var outputStream: OutputStream? = null
                    try {
                        inputStream = body.byteStream()
                        outputStream = resolver.openOutputStream(uri)

                        val buffer = ByteArray(4096)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream?.write(buffer, 0, read)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ReportDetailActivity, "Saved to Downloads", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ReportDetailActivity, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        inputStream?.close()
                        outputStream?.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReportDetailActivity, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
