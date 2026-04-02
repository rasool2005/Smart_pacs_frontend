package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PatientDetailsActivity : AppCompatActivity() {

    private lateinit var viewModel: AiReportViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ReportsAdapter
    private var patient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_details)
        
        Log.d("PatientDetails", "Activity Started")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[AiReportViewModel::class.java]

        // Get patient object from intent safely
        try {
            patient = intent.getSerializableExtra("PATIENT_OBJECT") as? Patient
            Log.d("PatientDetails", "Patient name: ${patient?.patient_name}")
        } catch (e: Exception) {
            Log.e("PatientDetails", "Error getting patient object", e)
        }

        setupPatientInfo()
        setupRecyclerView()
        setupObservers()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSchedule).setOnClickListener {
            val intent = Intent(this, ScheduleAppointmentActivity::class.java)
            intent.putExtra("PATIENT_NAME", patient?.patient_name ?: "")
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnAiScanner)?.setOnClickListener {
            val intent = Intent(this, AiScannerActivity::class.java)
            intent.putExtra("PATIENT_NAME", patient?.patient_name ?: "")
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnCancelSchedule).setOnClickListener {
            Toast.makeText(this, "Schedule Cancelled", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvSeeAllScans).setOnClickListener {
            val intent = Intent(this, AiScanHistoryActivity::class.java)
            intent.putExtra("PATIENT_NAME", patient?.patient_name ?: "")
            startActivity(intent)
        }
    }

    private fun setupPatientInfo() {
        if (patient == null) {
            Log.e("PatientDetails", "Patient data is NULL")
            return
        }
        
        patient?.let { p ->
            findViewById<TextView>(R.id.tvPatientName).text = p.patient_name
            findViewById<TextView>(R.id.tvPatientMeta).text = "MRN-${p.patient_id} • ${p.dob} • ${p.blood_type}"
            findViewById<TextView>(R.id.tvBreadcrumb).text = "Patients  /  ${p.patient_name}"
            
            val initials = p.patient_name.split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
            findViewById<TextView>(R.id.tvInitials).text = initials

            findViewById<TextView>(R.id.tvDob).text = p.dob
            findViewById<TextView>(R.id.tvPhone).text = p.phone_number
            findViewById<TextView>(R.id.tvEmail).text = p.email
            findViewById<TextView>(R.id.tvAddress).text = p.address
            findViewById<TextView>(R.id.tvBloodType).text = p.blood_type
            findViewById<TextView>(R.id.tvAllergies).text = p.allergies
        }
    }

    private fun setupRecyclerView() {
        val rvPatientScans = findViewById<RecyclerView>(R.id.rvPatientScans) ?: return
        adapter = ReportsAdapter(
            reportsList = emptyList(),
            patientNames = emptyList(),
            onDeleteClick = { selectedReport ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Scan")
                    .setMessage("Are you sure you want to delete this scan?")
                    .setPositiveButton("Delete") { _, _ ->
                        val userId = sessionManager.getUserId()
                        if (userId != -1) {
                            viewModel.deleteReport(userId, selectedReport.id)
                        } else {
                            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                        }
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
                
                intent.putExtra("prediction_results", predictionResponse)
                intent.putExtra("scan_type", selectedReport.examination_type)
                intent.putExtra("PATIENT_NAME", patient?.patient_name ?: "Unknown")
                intent.putExtra("is_history", true)
                
                if (!selectedReport.image_uri.isNullOrEmpty()) {
                    intent.putExtra("image_uri", selectedReport.image_uri)
                } else {
                    val imageResId = when (selectedReport.examination_type?.lowercase()) {
                        "ct scan", "ct" -> R.drawable.real_ct_scan
                        "mri", "mri brain" -> R.drawable.real_mri_scan
                        "x-ray", "xray", "x-ray chest" -> R.drawable.real_xray_chest
                        else -> R.drawable.img_mock_ct
                    }
                    intent.putExtra("image_res_id", imageResId)
                }
                
                startActivity(intent)
            }
        )
        rvPatientScans.layoutManager = LinearLayoutManager(this)
        rvPatientScans.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.reportsListState.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val allReports = resource.data ?: emptyList()
                        val targetName = patient?.patient_name?.trim()
                        
                        val filteredReports = if (!targetName.isNullOrEmpty()) {
                            allReports.filter { report ->
                                val imp = report.impression ?: ""
                                val obs = report.observation ?: ""
                                val find = report.finding_name ?: ""
                                imp.contains(targetName, ignoreCase = true) || 
                                obs.contains(targetName, ignoreCase = true) ||
                                find.contains(targetName, ignoreCase = true)
                            }.sortedByDescending { it.created_at }
                        } else {
                            emptyList()
                        }

                        updateHistoryUI(filteredReports, targetName ?: "Unknown")
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deleteReportState.collect { resource ->
                if (resource is Resource.Success) {
                    Toast.makeText(this@PatientDetailsActivity, "Scan deleted", Toast.LENGTH_SHORT).show()
                    refreshData()
                }
            }
        }
    }

    private fun updateHistoryUI(reports: List<AiReport>, pName: String) {
        val tvNoScans = findViewById<TextView>(R.id.tvNoScans) ?: return
        val rvPatientScans = findViewById<RecyclerView>(R.id.rvPatientScans) ?: return
        
        if (reports.isEmpty()) {
            tvNoScans.text = "No scan history found for this patient."
            tvNoScans.visibility = View.VISIBLE
            rvPatientScans.visibility = View.GONE
        } else {
            tvNoScans.visibility = View.GONE
            rvPatientScans.visibility = View.VISIBLE
            adapter.updateData(reports.take(3), listOf(pName))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            viewModel.getAiReports(userId)
        }
    }
}
