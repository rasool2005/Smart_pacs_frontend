package com.simats.smartpcas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StudiesActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudiesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: android.widget.EditText
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
        etSearch = findViewById(R.id.etSearchStudies)

        setupRecyclerView()
        setupSearch()
        
        // --- INSTANT LOAD FROM CACHE ---
        loadStudiesFromCache()

        // Back button logic
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Initialize bottom navigation
        setupBottomNavigation()
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        // Since Studies is not a main tab, we unselect all to show focus on content
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)
        findViewById<ImageView>(R.id.ivHome)?.setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome)?.setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivPatients)?.setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients)?.setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivSchedule)?.setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule)?.setTextColor(unselectedColor)
        findViewById<ImageView>(R.id.ivProfile)?.setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile)?.setTextColor(unselectedColor)
        findViewById<TextView>(R.id.tvAiChatLabel)?.setTextColor(unselectedColor)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filter(text: String) {
        val filteredList = if (text.isEmpty()) {
            fullStudyList
        } else {
            fullStudyList.filter {
                it.patient_name.contains(text, ignoreCase = true) ||
                it.study_type.contains(text, ignoreCase = true) ||
                it.note?.contains(text, ignoreCase = true) == true
            }
        }
        adapter.updateData(filteredList)
        findViewById<View>(R.id.llEmptyState).visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadStudiesFromCache() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val cache = sessionManager.getCache("studies_cache_$userId")
        
        if (!cache.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Study>>() {}.type
                val cachedList: List<Study> = Gson().fromJson(cache, type)
                
                if (cachedList.isNotEmpty()) {
                    val deletedIds = getDeletedStudyIds(userId)
                    fullStudyList = cachedList.filter { it.id.toString() !in deletedIds }
                    
                    if (fullStudyList.isNotEmpty()) {
                        findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                        adapter.updateData(fullStudyList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Always trigger a background refresh
        refreshAllData()
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
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        
        // Hide progress bar - user requested no loading interruption
        progressBar.visibility = View.GONE
        
        lifecycleScope.launch {
            // STEP 1: LOAD LOCAL REPORTS INSTANTLY TO SHOW LATEST SCANS
            try {
                val aiRepo = AiReportRepository(this@StudiesActivity)
                val localReports = aiRepo.getLocalReports(userId)
                val deletedIds = getDeletedStudyIds(userId)
                
                val localAiStudies = localReports.filter { !deletedIds.contains(it.id.toString()) }.map { report ->
                    mapReportToStudy(report)
                }
                
                if (localAiStudies.isNotEmpty() && fullStudyList.isEmpty()) {
                    fullStudyList = localAiStudies.sortedByDescending { it.created_at }
                    adapter.updateData(fullStudyList)
                    findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                }
            } catch (e: Exception) {}

            // STEP 2: BACKGROUND REFRESH FROM SERVER REPORTS ONLY
            try {
                val aiReportsDeferred = async {
                    try {
                        val aiRepo = AiReportRepository(this@StudiesActivity)
                        val aiResource = aiRepo.getReports(userId)
                        if (aiResource is Resource.Success) {
                            aiResource.data ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) { emptyList<AiReport>() }
                }

                // Wait for results
                val aiReports = withTimeoutOrNull(8000) { aiReportsDeferred.await() } ?: emptyList()
                
                val deletedIds = getDeletedStudyIds(userId)
                
                val aiStudies = aiReports.filter { it.id.toString() !in deletedIds }.map { mapReportToStudy(it) }
                val finalFetched = aiStudies.sortedByDescending { it.created_at }

                if (finalFetched.isNotEmpty()) {
                    fullStudyList = finalFetched
                    findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                    adapter.updateData(fullStudyList)
                    sessionManager.saveCache("studies_cache_$userId", Gson().toJson(fullStudyList))
                } else if (fullStudyList.isEmpty()) {
                    findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                if (fullStudyList.isEmpty()) {
                    findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun mapReportToStudy(report: AiReport): Study {
        val patientName = if (report.impression.startsWith("[Patient: ")) {
            report.impression.substringAfter("[Patient: ").substringBefore("]")
        } else "Unknown"

        return Study(
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

    private val temporarilyDeletedStudyIds = mutableSetOf<String>()

    private fun getDeletedStudyIds(userId: Int): Set<String> {
        return temporarilyDeletedStudyIds
    }

    private fun markStudyAsDeletedLocally(userId: Int, studyId: Int) {
        temporarilyDeletedStudyIds.add(studyId.toString())
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
