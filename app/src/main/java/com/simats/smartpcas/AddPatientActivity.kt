package com.simats.smartpcas

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.launch
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
        if (dob.isEmpty()) {
            Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate phone number using CountryCodePicker
        if (!ccp.isValidFullNumber) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Get full number with plus (e.g., +919391092540)
        val fullPhoneNumber = ccp.fullNumberWithPlus

        lifecycleScope.launch {
            try {
                val request = AddPatientRequest(
                    patient_name = patientName,
                    dob = dob,
                    phone_number = fullPhoneNumber,
                    address = address,
                    email = email,
                    blood_type = bloodType,
                    allergies = allergies
                )

                val response = ApiClient.apiService.addPatient(request)


                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@AddPatientActivity, "Patient added successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddPatientActivity, "Error: ${body?.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AddPatientActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPatientActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                etDob.setText(date)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}
