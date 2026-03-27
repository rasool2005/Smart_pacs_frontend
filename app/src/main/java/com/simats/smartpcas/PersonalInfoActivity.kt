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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDob: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etState: EditText
    private lateinit var etZipCode: EditText
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var sessionManager: SessionManager
    private var isUpdateMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_info)
        
        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        initViews()
        fetchPersonalInfo()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etDob.setOnClickListener {
            showDatePicker()
        }

        btnSaveChanges.setOnClickListener {
            saveChanges()
        }
    }

    private fun initViews() {
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etDob = findViewById(R.id.etDob)
        etAddress = findViewById(R.id.etAddress)
        etCity = findViewById(R.id.etCity)
        etState = findViewById(R.id.etState)
        etZipCode = findViewById(R.id.etZipCode)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        
        // Default state
        btnSaveChanges.text = "Save Changes"
    }

    private fun fetchPersonalInfo() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getPersonalInfo(userId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val info = response.body()?.data
                    if (info != null) {
                        isUpdateMode = true
                        btnSaveChanges.text = "Update Changes"
                        
                        etFirstName.setText(info.first_name)
                        etLastName.setText(info.last_name)
                        etEmail.setText(info.email)
                        etPhone.setText(info.phone_number)
                        etDob.setText(info.date_of_birth)
                        etAddress.setText(info.street_address)
                        etCity.setText(info.city)
                        etState.setText(info.state)
                        etZipCode.setText(info.zip_code)
                    }
                }
            } catch (e: Exception) {
                // Handle fetch error
            }
        }
    }

    private fun saveChanges() {
        val userId = sessionManager.getUserId()
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val city = etCity.text.toString().trim()
        val state = etState.text.toString().trim()
        val zip = etZipCode.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || 
            dob.isEmpty() || address.isEmpty() || city.isEmpty() || state.isEmpty() || zip.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = if (isUpdateMode) {
                    ApiClient.apiService.updatePersonalInfo(
                        user_id = userId,
                        first_name = firstName,
                        last_name = lastName,
                        email = email,
                        phone_number = phone,
                        date_of_birth = dob,
                        street_address = address,
                        city = city,
                        state = state,
                        zip_code = zip
                    )
                } else {
                    ApiClient.apiService.savePersonalInfo(
                        user_id = userId,
                        first_name = firstName,
                        last_name = lastName,
                        email = email,
                        phone_number = phone,
                        date_of_birth = dob,
                        street_address = address,
                        city = city,
                        state = state,
                        zip_code = zip
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@PersonalInfoActivity, body.message, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PersonalInfoActivity, body?.message ?: "Operation failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PersonalInfoActivity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PersonalInfoActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
