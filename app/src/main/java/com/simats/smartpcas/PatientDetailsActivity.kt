package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PatientDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_details)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Get patient object from intent
        val patient = intent.getSerializableExtra("PATIENT_OBJECT") as? Patient

        if (patient != null) {
            findViewById<TextView>(R.id.tvPatientName).text = patient.patient_name
            findViewById<TextView>(R.id.tvPatientMeta).text = "MRN-${patient.patient_id} • ${patient.dob} • ${patient.blood_type}"
            findViewById<TextView>(R.id.tvBreadcrumb).text = "Patients  /  ${patient.patient_name}"
            
            // Set initials
            val initials = patient.patient_name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
            findViewById<TextView>(R.id.tvInitials).text = initials

            // Detailed info
            findViewById<TextView>(R.id.tvDob).text = patient.dob
            findViewById<TextView>(R.id.tvPhone).text = patient.phone_number
            findViewById<TextView>(R.id.tvEmail).text = patient.email
            findViewById<TextView>(R.id.tvAddress).text = patient.address
            findViewById<TextView>(R.id.tvBloodType).text = patient.blood_type
            findViewById<TextView>(R.id.tvAllergies).text = patient.allergies
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSchedule).setOnClickListener {
            val intent = Intent(this, ScheduleAppointmentActivity::class.java)
            intent.putExtra("PATIENT_NAME", patient?.patient_name ?: "")
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnCancelSchedule).setOnClickListener {
            android.widget.Toast.makeText(this, "Schedule Cancelled", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnViewAll).setOnClickListener {
            startActivity(Intent(this, StudiesActivity::class.java))
        }

        // Handle CT Chest Card Click
        findViewById<MaterialCardView>(R.id.cvStudyCTChest).setOnClickListener {
            val intent = Intent(this, AiResultsActivity::class.java)
            intent.putExtra("scan_type", "CT Scan")
            intent.putExtra("image_res_id", R.drawable.real_ct_scan)
            startActivity(intent)
        }

        // Handle X-Ray Card Click
        findViewById<MaterialCardView>(R.id.cvStudyXray).setOnClickListener {
            val intent = Intent(this, AiResultsActivity::class.java)
            intent.putExtra("scan_type", "X-Ray")
            intent.putExtra("image_res_id", R.drawable.real_xray_chest)
            startActivity(intent)
        }

        // Handle MRI Card Click
        findViewById<MaterialCardView>(R.id.cvStudyMri).setOnClickListener {
            val intent = Intent(this, AiResultsActivity::class.java)
            intent.putExtra("scan_type", "MRI")
            intent.putExtra("image_res_id", R.drawable.real_mri)
            startActivity(intent)
        }
    }
}
