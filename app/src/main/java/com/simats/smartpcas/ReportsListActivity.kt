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
        // Start fetching reports immediately without blocking on patients list
        viewModel.getAiReports(userId)
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
                        // Only show progress bar if we have ZERO reports currently
                        if (adapter.itemCount == 0) {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        val reports = resource.data ?: emptyList()
                        val targetPatientName = intent.getStringExtra("PATIENT_NAME")
                        
                        // Extract and filter
                        val baseFiltered = if (!targetPatientName.isNullOrEmpty()) {
                            reports.filter { report ->
                                val imp = report.impression ?: ""
                                val pName = if (imp.startsWith("[Patient: ")) {
                                    imp.substringAfter("[Patient: ").substringBefore("]")
                                } else "Unknown Patient"
                                
                                val normalizedPName = if (pName == "null" || pName.isBlank()) "Unknown Patient" else pName
                                normalizedPName.equals(targetPatientName, ignoreCase = true)
                            }
                        } else {
                            reports
                        }
                        
                        // Final reports
                        val finalReports = baseFiltered

                        if (finalReports.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvReports.visibility = View.GONE
                            tvEmptyMessage.text = if (!targetPatientName.isNullOrEmpty()) 
                                "No reports found for $targetPatientName" else "No reports available."
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvReports.visibility = View.VISIBLE
                            adapter.updateData(finalReports, patientNamesList)
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if (adapter.itemCount == 0) {
                            llEmptyState.visibility = View.VISIBLE
                            tvEmptyMessage.text = resource.message ?: "Failed to load reports."
                        }
                    }
                }
            }
        }
        
        // Safety timeout: Hide progress bar after 10 seconds regardless of state
        lifecycleScope.launch {
            kotlinx.coroutines.delay(10000)
            if (progressBar.visibility == View.VISIBLE) {
                progressBar.visibility = View.GONE
                if (adapter.itemCount == 0) {
                    llEmptyState.visibility = View.VISIBLE
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
