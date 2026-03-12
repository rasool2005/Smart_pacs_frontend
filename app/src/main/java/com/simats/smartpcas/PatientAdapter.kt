package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientAdapter(
    private var patients: List<Patient>,
    private val onDeleteClick: (Patient) -> Unit,
    private val onItemClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    class PatientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPatientName)
        val tvMeta: TextView = view.findViewById(R.id.tvPatientMeta)
        val btnViewDetails: TextView = view.findViewById(R.id.btnViewDetails)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeletePatient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        holder.tvName.text = patient.patient_name
        val meta = "MRN-${patient.patient_id} • ${patient.dob} • ${patient.blood_type}"
        holder.tvMeta.text = meta
        
        holder.itemView.setOnClickListener { onItemClick(patient) }
        holder.btnViewDetails.setOnClickListener { onItemClick(patient) }
        
        holder.ivDelete.setOnClickListener {
            onDeleteClick(patient)
        }
    }

    override fun getItemCount() = patients.size

    fun updateData(newPatients: List<Patient>) {
        patients = newPatients
        notifyDataSetChanged()
    }
}
