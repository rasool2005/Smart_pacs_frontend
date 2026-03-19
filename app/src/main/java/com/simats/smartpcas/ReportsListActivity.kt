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

class ReportsListActivity : AppCompatActivity() {

    private lateinit var viewModel: AiReportViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ReportsAdapter
    private var patientNamesList: List<String> = emptyList()

    private lateinit var rvReports: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: View
    private lateinit var tvEmptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reports_list)

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

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        val userId = sessionManager.getUserId()
        if (userId != -1) {
            fetchPatientNamesAndReports(userId)
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPatientNamesAndReports(userId: Int) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                // Pass the userId to getPatients as required by ApiService
                val response = ApiClient.apiService.getPatients(userId)
                if (response.isSuccessful && response.body() != null) {
                    val patients = response.body()?.patients ?: emptyList()
                    val names = patients.map { it.patient_name }.filter { it.isNotBlank() }
                    patientNamesList = names.ifEmpty { listOf("Unknown Patient") }
                } else {
                    patientNamesList = listOf("Unknown Patient")
                }
            } catch (e: Exception) {
                patientNamesList = listOf("Unknown Patient")
            }
            
            // Proceed to fetch reports after getting names
            viewModel.getAiReports(userId)
        }
    }

    private fun setupRecyclerView() {
        adapter = ReportsAdapter(
            reportsList = emptyList(),
            patientNames = emptyList(),
            onDeleteClick = { selectedReport ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to permanently delete this report?")
                    .setPositiveButton("Delete") { _, _ ->
                        val userId = sessionManager.getUserId()
                        if (userId != -1) {
                            adapter.removeItem(selectedReport)
                            viewModel.deleteReport(userId, selectedReport.id)
                        }
                        if (adapter.itemCount == 0) {
                            llEmptyState.visibility = View.VISIBLE
                            rvReports.visibility = View.GONE
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onReportClick = { selectedReport ->
                // ✅ Changed from AiResultsActivity back to ReportDetailActivity to show the structured report
                val intent = Intent(this, ReportDetailActivity::class.java)
                intent.putExtra("report_data", selectedReport)
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
                        val targetPatientName = intent.getStringExtra("PATIENT_NAME")
                        
                        val filteredReports = if (!targetPatientName.isNullOrEmpty()) {
                            reports.filter { report ->
                                val imp = report.impression ?: ""
                                val pName = if (imp.startsWith("[Patient: ")) {
                                    imp.substringAfter("[Patient: ").substringBefore("]")
                                } else {
                                    "Legacy Report"
                                }
                                pName == targetPatientName
                            }
                        } else {
                            reports
                        }
                        
                        if (filteredReports.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvReports.visibility = View.GONE
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvReports.visibility = View.VISIBLE
                            
                            adapter.updateData(filteredReports, patientNamesList)
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
                when (resource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@ReportsListActivity, "Report deleted successfully", Toast.LENGTH_SHORT).show()
                        val userId = sessionManager.getUserId()
                        if (userId != -1) {
                            fetchPatientNamesAndReports(userId)
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                    }
                    else -> {}
                }
            }
        }
    }
}
