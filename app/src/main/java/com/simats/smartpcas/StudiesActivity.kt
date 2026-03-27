package com.simats.smartpcas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar
import kotlinx.coroutines.launch

class StudiesActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudiesAdapter
    private lateinit var progressBar: ProgressBar
    private var fullStudyList: List<Study> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_studies)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        recyclerView = findViewById(R.id.rvStudies)
        progressBar = findViewById(R.id.progressBar)

        setupRecyclerView()
        refreshAllData()

        // Back button logic
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Initialize bottom navigation
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = StudiesAdapter(
            studies = emptyList(),
            onStudyClick = { study ->
                openDetails(study)
            },
            onDeleteClick = { study ->
                showDeleteConfirmation(study)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun refreshAllData() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) {
            findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // 1. Fetch server studies
                val studiesResponse = ApiClient.apiService.getStudies(userId)
                val serverStudies = if (studiesResponse.isSuccessful) {
                    studiesResponse.body()?.studies ?: emptyList()
                } else emptyList()

                // 2. Fetch local AI reports and convert to Study objects
                val aiRepo = AiReportRepository(this@StudiesActivity)
                val aiResource = aiRepo.getReports(userId)
                val aiReports = (aiResource as? Resource.Success)?.data ?: emptyList()
                
                val aiStudies = aiReports.map { report ->
                    val patientName = if (report.impression.startsWith("[Patient: ")) {
                        report.impression.substringAfter("[Patient: ").substringBefore("]")
                    } else "Unknown"

                    Study(
                        id = report.id,
                        patient_name = patientName,
                        study_type = report.examination_type,
                        study_date = report.created_at.split("T").firstOrNull() ?: "",
                        study_time = report.created_at.split("T").getOrNull(1)?.take(5) ?: "",
                        status = "Completed",
                        note = report.finding_name,
                        created_at = report.created_at,
                        image_uri = report.image_uri,
                        is_ai = true,
                        ai_report = report
                    )
                }

                // 3. Combine and Filter deleted
                val combined = (serverStudies + aiStudies).sortedByDescending { it.created_at }
                val deletedIds = getDeletedStudyIds(userId)
                fullStudyList = combined.filter { it.id.toString() !in deletedIds }

                progressBar.visibility = View.GONE
                if (fullStudyList.isEmpty()) {
                    findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
                    adapter.updateData(emptyList())
                } else {
                    findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                    adapter.updateData(fullStudyList)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
            }
        }
    }

    private fun fetchStudies() {
        refreshAllData()
    }

    private fun openDetails(study: Study) {
        if (study.is_ai && study.ai_report != null) {
            val report = study.ai_report
            val intent = Intent(this, AiResultsActivity::class.java)
            
            // Reconstruct PredictionResponse to use the premium Results UI
            val prediction = PredictionResponse(
                status = "success",
                scan_type = report.examination_type,
                confidence_score = report.confidence_score,
                confidence_level = report.confidence_level,
                message = "History Scan Result",
                findings = listOf(
                    AiFinding(
                        title = report.finding_name,
                        location = report.location,
                        description = report.observation,
                        confidence = report.confidence_score,
                        severity = report.severity
                    )
                )
            )
            
            intent.putExtra("prediction_results", prediction)
            intent.putExtra("image_uri", report.image_uri)
            intent.putExtra("is_history", true) // Prevent re-saving to history
            intent.putExtra("PATIENT_NAME", study.patient_name)
            intent.putExtra("scan_type", report.examination_type)
            startActivity(intent)
        } else {
            val intent = Intent(this, StudyDetailsActivity::class.java)
            intent.putExtra("STUDY_TITLE", study.study_type)
            intent.putExtra("PATIENT_INFO", study.patient_name)
            intent.putExtra("STUDY_DATE", study.study_date)
            intent.putExtra("CRITICALITY", study.status)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmation(study: Study) {
        AlertDialog.Builder(this)
            .setTitle("Delete Study")
            .setMessage("Are you sure you want to delete this study?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStudy(study)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getDeletedStudyIds(userId: Int): Set<String> {
        val prefs = getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("deleted_studies_$userId", emptySet()) ?: emptySet()
    }

    private fun markStudyAsDeletedLocally(userId: Int, studyId: Int) {
        val prefs = getSharedPreferences("study_prefs", Context.MODE_PRIVATE)
        val deletedIds = getDeletedStudyIds(userId).toMutableSet()
        deletedIds.add(studyId.toString())
        prefs.edit().putStringSet("deleted_studies_$userId", deletedIds).apply()
    }

    private fun deleteStudy(study: Study) {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Mark as deleted locally so it stays gone immediately
        markStudyAsDeletedLocally(userId, study.id)
        
        // 2. Refresh UI immediately to show it's gone
        refreshAllData()
        Toast.makeText(this, "Deleting study...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                if (study.is_ai) {
                    val aiRepo = AiReportRepository(this@StudiesActivity)
                    aiRepo.deleteReport(userId, study.id)
                } else {
                    ApiClient.apiService.deleteStudy(study.id)
                }
            } catch (e: Exception) {
                // Secondary check: log error but local deletion stays
            }
        }
    }
}
