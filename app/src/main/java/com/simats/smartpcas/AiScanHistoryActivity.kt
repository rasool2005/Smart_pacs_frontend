package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AiScanHistoryActivity : AppCompatActivity() {

    private lateinit var viewModel: AiReportViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ReportsAdapter
    
    private lateinit var rvReports: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: View
    private lateinit var tvEmptyMessage: TextView
    private var targetPatientName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_scan_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvReports = findViewById(R.id.rvReports)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        targetPatientName = intent.getStringExtra("PATIENT_NAME")

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        setupRecyclerView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            viewModel.getAiReports(userId)
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReportsAdapter(
            reportsList = emptyList(),
            patientNames = emptyList(),
            onDeleteClick = { selectedReport ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Scan History")
                    .setMessage("Are you sure you want to permanently delete this scan?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteReport(selectedReport.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onReportClick = { selectedReport ->
                val intent = Intent(this, AiResultsActivity::class.java)
                
                val finding = AiFinding(
                    title = selectedReport.finding_name ?: "Normal",
                    location = selectedReport.location ?: "N/A",
                    description = selectedReport.observation ?: "No observation recorded.",
                    confidence = selectedReport.confidence_score ?: 0.0,
                    severity = selectedReport.severity ?: "Low"
                )
                
                val predictionResponse = PredictionResponse(
                    status = "success",
                    scan_type = selectedReport.examination_type,
                    confidence_score = selectedReport.confidence_score,
                    confidence_level = selectedReport.confidence_level,
                    message = "Analysis loaded from history.",
                    findings = listOf(finding)
                )
                
                // Extract patient name from impression for better display in Results
                val imp = selectedReport.impression ?: ""
                val patientName = if (imp.startsWith("[Patient: ")) {
                    imp.substringAfter("[Patient: ").substringBefore("]")
                } else {
                    "Unknown Patient"
                }
                
                intent.putExtra("prediction_results", predictionResponse)
                intent.putExtra("scan_type", selectedReport.examination_type)
                intent.putExtra("PATIENT_NAME", patientName)
                intent.putExtra("is_history", true)
                
                if (!selectedReport.image_uri.isNullOrEmpty()) {
                    intent.putExtra("image_uri", selectedReport.image_uri)
                } else {
                    val imageResId = when (selectedReport.examination_type?.lowercase()) {
                        "ct scan", "ct" -> R.drawable.real_ct_scan
                        "mri", "mri brain" -> R.drawable.real_mri
                        "x-ray", "xray", "x-ray chest" -> R.drawable.real_xray_chest
                        else -> R.drawable.img_mock_ct
                    }
                    intent.putExtra("image_res_id", imageResId)
                }
                
                startActivity(intent)
            }
        )
        rvReports.layoutManager = LinearLayoutManager(this)
        rvReports.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.reportsListState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        rvReports.visibility = View.GONE
                        llEmptyState.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        val reports = resource.data ?: emptyList()
                        
                        val filteredReports = if (!targetPatientName.isNullOrEmpty()) {
                            reports.filter { report ->
                                val imp = report.impression ?: ""
                                val obs = report.observation ?: ""
                                val find = report.finding_name ?: ""
                                imp.contains(targetPatientName!!, ignoreCase = true) || 
                                obs.contains(targetPatientName!!, ignoreCase = true) ||
                                find.contains(targetPatientName!!, ignoreCase = true)
                            }
                        } else {
                            reports
                        }
                        
                        if (filteredReports.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvReports.visibility = View.GONE
                            tvEmptyMessage.text = if (targetPatientName.isNullOrEmpty()) 
                                "No scan history available." else "No scan history found for $targetPatientName."
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvReports.visibility = View.VISIBLE
                            adapter.updateData(filteredReports, listOf(targetPatientName ?: "Unknown"))
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                        tvEmptyMessage.text = resource.message ?: "Failed to load reports."
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deleteReportState.collect { resource ->
                if (resource is Resource.Success) {
                    Toast.makeText(this@AiScanHistoryActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    val userId = sessionManager.getUserId()
                    if (userId != -1) viewModel.getAiReports(userId)
                }
            }
        }
    }
}
