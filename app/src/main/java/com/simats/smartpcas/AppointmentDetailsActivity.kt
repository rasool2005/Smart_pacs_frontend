package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AppointmentDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_details)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get study object from intent
        val study = intent.getSerializableExtra("STUDY_OBJECT") as? Study

        if (study != null) {
            populateDetails(study)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, ScheduleAppointmentActivity::class.java)
            intent.putExtra("PATIENT_NAME", study?.patient_name)
            // Can pass more details for full edit if needed
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            Toast.makeText(this, "Appointment Cancelled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateDetails(study: Study) {
        findViewById<TextView>(R.id.tvStudyTypeHeader).text = study.study_type
        findViewById<TextView>(R.id.tvPatientName).text = study.patient_name
        findViewById<TextView>(R.id.tvPatientId).text = "MRN-${study.id}"
        findViewById<TextView>(R.id.tvDate).text = study.study_date
        findViewById<TextView>(R.id.tvTime).text = study.study_time
        findViewById<TextView>(R.id.tvNotes).text = if (study.note.isNullOrEmpty()) "No notes available." else study.note

        // Update status card based on status
        val statusCard = findViewById<MaterialCardView>(R.id.statusCard)
        val statusIcon = findViewById<ImageView>(R.id.ivStatusIcon)
        val statusLabel = findViewById<TextView>(R.id.tvStatusLabel)
        val statusSub = findViewById<TextView>(R.id.tvStatusSub)

        if (study.status.lowercase() == "confirmed") {
            statusLabel.text = "Status: Confirmed"
            statusSub.text = "Patient has been notified"
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.critical_red_light)) // Reusing light red/green if available
            // Using direct colors if theme colors are limited
            statusCard.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FDF4"))
            statusIcon.setImageResource(R.drawable.ic_check_circle)
            statusIcon.setColorFilter(android.graphics.Color.parseColor("#16A34A"))
            statusLabel.setTextColor(android.graphics.Color.parseColor("#16A34A"))
            statusSub.setTextColor(android.graphics.Color.parseColor("#16A34A"))
        } else {
            statusLabel.text = "Status: Pending"
            statusSub.text = "Waiting for verification"
            statusCard.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFBEB"))
            statusIcon.setImageResource(R.drawable.ic_time)
            statusIcon.setColorFilter(android.graphics.Color.parseColor("#D97706"))
            statusLabel.setTextColor(android.graphics.Color.parseColor("#D97706"))
            statusSub.setTextColor(android.graphics.Color.parseColor("#D97706"))
        }
    }
}
