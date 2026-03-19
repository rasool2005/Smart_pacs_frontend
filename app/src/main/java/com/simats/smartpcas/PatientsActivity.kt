package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class PatientsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PatientAdapter
    private lateinit var progressBar: ProgressBar
    private var fullPatientList: List<Patient> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patients)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        recyclerView = findViewById(R.id.rvPatients)
        progressBar = findViewById(R.id.progressBar)

        setupRecyclerView()
        setupSearch()
        
        // Ensure UI starts clean
        fullPatientList = emptyList()
        adapter.updateData(emptyList())
        
        fetchPatients()
        setupClickListeners()
        updateBottomNavSelection()
    }

    private fun setupRecyclerView() {
        adapter = PatientAdapter(
            patients = emptyList(),
            onDeleteClick = { patient ->
                showDeleteConfirmation(patient)
            },
            onItemClick = { patient ->
                openDetails(patient)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmation(patient: Patient) {
        AlertDialog.Builder(this)
            .setTitle("Delete Patient")
            .setMessage("Are you sure you want to delete ${patient.patient_name}? This will also remove their associated history.")
            .setPositiveButton("Delete") { _, _ ->
                deletePatient(patient)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun getDeletedPatientIds(): Set<String> {
        val userId = SessionManager(this).getUserId()
        val prefs = getSharedPreferences("doctor_isolation_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("deleted_patients_$userId", emptySet()) ?: emptySet()
    }

    private fun markPatientAsDeletedLocally(patientId: Int) {
        val userId = SessionManager(this).getUserId()
        val prefs = getSharedPreferences("doctor_isolation_prefs", Context.MODE_PRIVATE)
        val deletedIds = getDeletedPatientIds().toMutableSet()
        deletedIds.add(patientId.toString())
        prefs.edit().putStringSet("deleted_patients_$userId", deletedIds).apply()
    }

    private fun deletePatient(patient: Patient) {
        // Mark as deleted in local storage
        markPatientAsDeletedLocally(patient.patient_id)
        
        // Remove from current UI list
        fullPatientList = fullPatientList.filter { it.patient_id != patient.patient_id }
        filterPatients(findViewById<EditText>(R.id.etSearchPatients).text.toString())
        
        lifecycleScope.launch {
            try {
                // Background attempt to delete from server
                ApiClient.apiService.deletePatient(patient.patient_id)
            } catch (e: Exception) {}
        }
    }

    private fun setupClickListeners() {
        // Add Patient
        findViewById<MaterialCardView>(R.id.btnAddPatient).setOnClickListener {
            startActivity(Intent(this, AddPatientActivity::class.java))
        }

        // Custom Bottom Navigation
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navPatients).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.navAiChat).setOnClickListener {
            startActivity(Intent(this, AiChatActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, FollowUpActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun updateBottomNavSelection() {
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        // Set Patients as selected
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(brandBlue)
        findViewById<TextView>(R.id.tvPatients).setTextColor(brandBlue)

        // Ensure others are unselected
        findViewById<ImageView>(R.id.ivHome).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivProfile).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile).setTextColor(unselectedColor)
    }

    private fun fetchPatients() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        
        if (userId == -1) {
            updateUiWithEmptyState()
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Fetch ONLY for this specific doctor account
                val response = ApiClient.apiService.getPatients(userId)
                progressBar.visibility = View.GONE
                
                if (response.isSuccessful && response.body() != null) {
                    val patientsResponse = response.body()!!
                    if (patientsResponse.status == "success") {
                        val newPatients = patientsResponse.patients ?: emptyList()
                        
                        // Show all patients returned by the server (server already filters by userId)
                        val deletedIds = getDeletedPatientIds()
                        
                        fullPatientList = newPatients.filter { 
                            it.patient_id.toString() !in deletedIds 
                        }
                        
                        if (fullPatientList.isEmpty()) {
                            updateUiWithEmptyState()
                        } else {
                            findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            val etSearchPatients = findViewById<EditText>(R.id.etSearchPatients)
                            filterPatients(etSearchPatients.text.toString())
                        }
                    } else {
                        updateUiWithEmptyState()
                    }
                } else {
                    updateUiWithEmptyState()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                updateUiWithEmptyState()
            }
        }
    }

    private fun updateUiWithEmptyState() {
        fullPatientList = emptyList()
        adapter.updateData(emptyList())
        findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun setupSearch() {
        val etSearchPatients = findViewById<EditText>(R.id.etSearchPatients)
        etSearchPatients.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPatients(s.toString())
            }
        })

        if (intent.getBooleanExtra("FOCUS_SEARCH", false)) {
            etSearchPatients.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearchPatients, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun filterPatients(query: String) {
        val llEmptyState = findViewById<View>(R.id.llEmptyState)
        if (query.isEmpty()) {
            adapter.updateData(fullPatientList)
            if (fullPatientList.isEmpty()) {
                llEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                llEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            return
        }
        val lowerCaseQuery = query.lowercase()
        val filteredList = fullPatientList.filter {
            (it.patient_name ?: "").lowercase().contains(lowerCaseQuery) ||
            (it.patient_id.toString()).lowercase().contains(lowerCaseQuery)
        }
        adapter.updateData(filteredList)
        
        if (filteredList.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun openDetails(patient: Patient) {
        val intent = Intent(this, PatientDetailsActivity::class.java)
        intent.putExtra("PATIENT_OBJECT", patient)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
        fetchPatients()
    }
}
