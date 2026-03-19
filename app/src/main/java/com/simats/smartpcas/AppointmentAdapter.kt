package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(
    private var appointments: List<Study>,
    private val onDeleteClick: (Study) -> Unit,
    private val onItemClick: (Study) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        val tvStudyType: TextView = view.findViewById(R.id.tvStudyType)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteAppointment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.tvPatientName.text = appointment.patient_name
        holder.tvStudyType.text = appointment.study_type
        holder.tvDate.text = appointment.study_date
        holder.tvTime.text = appointment.study_time

        val sessionManager = SessionManager(holder.itemView.context)
        val currentStatus = sessionManager.getStudyStatus(appointment.id) ?: appointment.status

        // Dynamic status display
        if (currentStatus.lowercase() == "confirmed") {
            holder.tvTag.text = "Confirmed"
            holder.tvTag.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_e8f5e9_rounded)
            holder.tvTag.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.teal_700))
        } else {
            holder.tvTag.text = "Pending"
            holder.tvTag.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_fff3e0_rounded)
            holder.tvTag.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.high_orange))
        }

        holder.itemView.setOnClickListener { onItemClick(appointment) }
        
        holder.ivDelete.setOnClickListener {
            onDeleteClick(appointment)
        }
    }

    override fun getItemCount() = appointments.size

    fun updateData(newAppointments: List<Study>) {
        appointments = newAppointments.distinctBy { it.id }
        notifyDataSetChanged()
    }
}
