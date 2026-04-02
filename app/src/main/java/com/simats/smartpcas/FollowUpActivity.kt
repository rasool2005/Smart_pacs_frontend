package com.simats.smartpcas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class FollowUpActivity : BaseActivity() {

    private lateinit var rvUpcoming: RecyclerView
    private lateinit var adapter: AppointmentAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var tvConfirmedCount: TextView
    private lateinit var tvPendingCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follow_up)

        sessionManager = SessionManager(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        rvUpcoming = findViewById(R.id.rvUpcomingAppointments)
        tvConfirmedCount = findViewById(R.id.tvConfirmedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)

        setupRecyclerView()
        fetchAppointments()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnScheduleNew).setOnClickListener {
            startActivity(Intent(this, ScheduleAppointmentActivity::class.java))
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, ScheduleAppointmentActivity::class.java))
        }

        // Link Follow-Up Required to Schedule Page
        findViewById<MaterialCardView>(R.id.cvFollowUpReq1).setOnClickListener {
            startActivity(Intent(this, ScheduleAppointmentActivity::class.java))
        }

        setupBottomNavigation()
        updateBottomNavSelection()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(
            emptyList(),
            onDeleteClick = { appointment ->
                showDeleteConfirmationDialog(appointment)
            },
            onItemClick = { appointment ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("STUDY_OBJECT", appointment)
                startActivity(intent)
            }
        )
        rvUpcoming.layoutManager = LinearLayoutManager(this)
        rvUpcoming.adapter = adapter
    }

    private val temporarilyDeletedIds = mutableSetOf<String>()

    private fun getDeletedAppointmentIds(): Set<String> {
        return temporarilyDeletedIds
    }

    private fun markAppointmentAsDeletedLocally(studyId: Int) {
        temporarilyDeletedIds.add(studyId.toString())
    }

    private fun showDeleteConfirmationDialog(appointment: Study) {
        AlertDialog.Builder(this)
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete this appointment?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAppointment(appointment.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAppointment(studyId: Int) {
        // --- LOCAL FIX START ---
        // 1. Mark as deleted locally so it stays gone
        markAppointmentAsDeletedLocally(studyId)
        
        // 2. Remove from current UI to show immediate feedback
        fetchAppointments() 
        Toast.makeText(this, "Deleting appointment...", Toast.LENGTH_SHORT).show()
        // --- LOCAL FIX END ---

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.deleteStudy(studyId)
                if (response.isSuccessful) {
                    Toast.makeText(this@FollowUpActivity, "Appointment deleted from server", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Network error is fine since we have local persistence
            }
        }
    }

    private fun fetchAppointments() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getStudies(userId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val studies = body.studies
                    
                    // Filter out locally deleted appointments
                    val deletedIds = getDeletedAppointmentIds()
                    val filteredStudies = studies.filter { it.id.toString() !in deletedIds }
                    
                    adapter.updateData(filteredStudies)
                    
                    var pendingCount = 0
                    var confirmedCount = 0
                    
                    filteredStudies.forEach { study ->
                        val currentStatus = sessionManager.getStudyStatus(study.id) ?: study.status
                        if (currentStatus.equals("Confirmed", ignoreCase = true)) {
                            confirmedCount++
                        } else {
                            pendingCount++
                        }
                    }
                    
                    tvConfirmedCount.text = confirmedCount.toString()
                    tvPendingCount.text = pendingCount.toString()
                } else {
                    // Fail silently but clear current list if server error
                }
            } catch (e: Exception) {
                // Handle network error
            }
        }
    }

    private fun updateBottomNavSelection() {
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(brandBlue)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(brandBlue)

        findViewById<ImageView>(R.id.ivHome).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivProfile).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile).setTextColor(unselectedColor)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
        fetchAppointments()
    }
}
