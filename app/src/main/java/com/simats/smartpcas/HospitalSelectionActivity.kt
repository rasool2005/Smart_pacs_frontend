package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HospitalSelectionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hospital_selection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rvHospitals = findViewById<RecyclerView>(R.id.rvHospitals)
        rvHospitals.layoutManager = LinearLayoutManager(this)

        val hospitals = listOf(
            Hospital("1", "Apollo Hospitals", "General Hospital", "21, Greams Lane, Off Greams Road,\nChennai", "+91 44 2829 3333", "enquiry@apollohospitals.com"),
            Hospital("2", "Fortis Malar Hospital", "Medical Center", "52, 1st Main Road, Gandhi Nagar, Adyar,\nChennai", "+91 44 4289 2222", "malar@fortishealthcare.com"),
            Hospital("3", "MIOT International", "Teaching Hospital", "4/112, Mount Poonamallee\nRoad, Manapakkam, Chennai", "+91 44 4200 2288", "contactus@miotinternational.com"),
            Hospital("4", "Kauvery Hospital", "Specialty Hospital", "81, TTK Road, Alwarpet, Chennai", "+91 44 4000 6000", "contactchennai@kauveryhospitals.com"),
            Hospital("5", "Vijaya Hospital", "Specialty Hospital", "180, NSK Salai, Vadapalani, Chennai", "+91 44 2471 1000", "mail@vijayahospital.com"),
            Hospital("6", "Sri Ramachandra Medical Centre", "Teaching Hospital", "No.1, Ramachandra Nagar, Porur,\nChennai", "+91 44 4592 4444", "enquiry@sriramachandra.edu.in"),
            Hospital("7", "Saveetha Medical College & Hospital", "Teaching Hospital", "Saveetha Nagar, Thandalam, Chennai", "+91 44 6681 1000", "smch@saveetha.com")
        )

        val adapter = HospitalAdapter(hospitals) { hospital ->
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("hospital_id", hospital.id)
            intent.putExtra("hospital_name", hospital.name)
            startActivity(intent)
        }
        rvHospitals.adapter = adapter
    }
}
