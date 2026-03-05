package com.simats.smartpcas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class ScheduleAppointmentActivity : AppCompatActivity() {

    private lateinit var etPatientName: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etStudyType: EditText
    private lateinit var etNotes: EditText
    private lateinit var sessionManager: SessionManager

    private val studyTypes = arrayOf(
        "CT Chest",
        "CT Abdomen",
        "CT Head",
        "MRI Brain",
        "MRI Spine",
        "X-Ray Chest",
        "X-Ray Abdomen"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule_appointment)

        sessionManager = SessionManager(this)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etPatientName = findViewById(R.id.etPatientName)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        etStudyType = findViewById(R.id.etStudyType)
        etNotes = findViewById(R.id.etNotes)

        // Get patient name from intent if passed
        val patientNameExtra = intent.getStringExtra("PATIENT_NAME")
        if (!patientNameExtra.isNullOrEmpty()) {
            etPatientName.setText(patientNameExtra)
            // Optional: disable editing if you want it to be fixed for this flow
            // etPatientName.isEnabled = false 
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etStudyType.setFocusable(false)
        etStudyType.setOnClickListener {
            showStudyTypeDialog()
        }

        etDate.setOnClickListener {
            showDatePicker()
        }

        etTime.setOnClickListener {
            showTimePicker()
        }

        findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            val patientName = etPatientName.text.toString().trim()
            val studyType = etStudyType.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (patientName.isEmpty()) {
                Toast.makeText(this, "Please enter patient name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (studyType.isEmpty()) {
                Toast.makeText(this, "Please select study type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (date.isEmpty()) {
                Toast.makeText(this, "Please select date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (time.isEmpty()) {
                Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveAppointment(
                userId = userId,
                patientName = patientName,
                studyType = studyType,
                studyDate = date,
                studyTime = time,
                note = if (notes.isEmpty()) null else notes
            )
        }
    }

    private fun saveAppointment(
        userId: Int,
        patientName: String,
        studyType: String,
        studyDate: String,
        studyTime: String,
        note: String?
    ) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.scheduleAppointment(
                    user_id = userId,
                    patient_name = patientName,
                    study_type = studyType,
                    study_date = studyDate,
                    study_time = studyTime,
                    note = note
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@ScheduleAppointmentActivity, "Study saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ScheduleAppointmentActivity, "Error: ${body?.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ScheduleAppointmentActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScheduleAppointmentActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStudyTypeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose study type...")
        builder.setItems(studyTypes) { _, which ->
            etStudyType.setText(studyTypes[which])
        }
        builder.show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                etDate.setText(date)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val hourIn12Format = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val time = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12Format, selectedMinute, amPm)
                etTime.setText(time)
            },
            hour,
            minute,
            false
        )
        timePickerDialog.show()
    }
}
