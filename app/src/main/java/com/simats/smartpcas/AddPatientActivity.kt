package com.simats.smartpcas

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.hbb20.CountryCodePicker
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPatientActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etDob: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBloodType: EditText
    private lateinit var etAllergies: EditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_patient)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        initViews()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etDob.setOnClickListener {
            showDatePicker()
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            validateAndSave()
        }
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etDob = findViewById(R.id.etDob)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        etBloodType = findViewById(R.id.etBloodType)
        etAllergies = findViewById(R.id.etAllergies)
        ccp = findViewById(R.id.ccp)
        progressBar = findViewById(R.id.progressBar)
        btnSave = findViewById(R.id.btnSave)
        
        // Attach the EditText to the CountryCodePicker for automatic validation
        ccp.registerCarrierNumberEditText(etPhone)
    }

    private fun validateAndSave() {
        val patientName = etFullName.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val bloodType = etBloodType.text.toString().trim()
        val allergies = etAllergies.text.toString().trim()

        if (patientName.isEmpty()) {
            Toast.makeText(this, "Please enter patient name", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Patient Name (Only alphabetic characters allowed)
        val nameRegex = Regex("^[a-zA-Z\\s]+$")
        if (!nameRegex.matches(patientName)) {
            Toast.makeText(this, "Invalid patient name. Only letters are allowed.", Toast.LENGTH_SHORT).show()
            return
        }
        if (dob.isEmpty()) {
            Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate DOB: no today, no future, max 2026
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.parse(dob)
            val calendar = Calendar.getInstance()
            
            // Today at midnight
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            // Max allowed (End of 2026)
            val maxAllowed = Calendar.getInstance()
            maxAllowed.set(2026, Calendar.DECEMBER, 31, 23, 59, 59)

            if (selectedDate != null) {
                if (selectedDate.after(today.time)) {
                    Toast.makeText(this, "Date of Birth cannot be in the future", Toast.LENGTH_SHORT).show()
                    return
                }
                if (selectedDate.after(maxAllowed.time)) {
                    Toast.makeText(this, "Date of Birth cannot be after 2026", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Email
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter the email address", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Blood Group
        val validBloodGroups = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        if (bloodType.isNotEmpty() && !validBloodGroups.contains(bloodType.uppercase())) {
            Toast.makeText(this, "Please enter a valid blood group (e.g. A+, O-, AB+)", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Allergies (Alphanumeric and spaces only, min 3 chars if provided)
        val alphaNumericRegex = Regex("^[a-zA-Z0-9\\s]*$")
        if (allergies.isNotEmpty()) {
            if (allergies.length < 3) {
                Toast.makeText(this, "Allergies info must be at least 3 characters", Toast.LENGTH_SHORT).show()
                return
            }
            if (!alphaNumericRegex.matches(allergies)) {
                Toast.makeText(this, "Allergies can only contain words and numbers", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Validate phone number using CountryCodePicker
        if (!ccp.isValidFullNumber) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Get full number without plus for better backend compatibility
        val fullPhoneNumber = ccp.fullNumber

        val userId = SessionManager(this).getUserId()
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.addPatient(
                    doctor_id = userId,
                    patient_name = patientName,
                    dob = dob,
                    phone_number = fullPhoneNumber,
                    address = address,
                    email = email,
                    blood_type = bloodType.uppercase(),
                    allergies = allergies
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@AddPatientActivity, "Patient added successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val errorMsg = body?.message ?: "Server returned failure status"
                        Toast.makeText(this@AddPatientActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var friendlyError = "Server Error ${response.code()}"
                    
                    try {
                        val json = JsonParser.parseString(errorBody).asJsonObject
                        if (json.has("message") && json.get("message").isJsonPrimitive) {
                            friendlyError = json.get("message").asString
                        } else if (json.has("errors")) {
                            val errors = json.get("errors").asJsonObject
                            if (errors.has("email")) {
                                friendlyError = errors.get("email").asJsonArray.get(0).asString
                            } else if (errors.entrySet().isNotEmpty()) {
                                // Just pick the first error found
                                val firstEntry = errors.entrySet().iterator().next()
                                friendlyError = firstEntry.value.asJsonArray.get(0).asString
                            }
                        }
                    } catch (e: Exception) {
                        // Keep default fallback
                        if (!errorBody.isNullOrBlank()) friendlyError += ": $errorBody"
                    }
                    
                    Toast.makeText(this@AddPatientActivity, friendlyError, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPatientActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        // Default to yesterday if today session is 2026+
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                etDob.setText(date)
            },
            year,
            month,
            day
        )
        
        // Restrict to past dates only (excluding today)
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        
        // Extra check for 2026 as requested
        val maxCalendar = Calendar.getInstance()
        maxCalendar.set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
        if (calendar.timeInMillis > maxCalendar.timeInMillis) {
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        }
        
        datePickerDialog.show()
    }
}
