package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsAdapter(
    private var reportsList: List<AiReport>,
    private var patientNames: List<String>,
    private val onReportClick: (AiReport) -> Unit
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    fun updateData(newReports: List<AiReport>, newPatientNames: List<String>) {
        reportsList = newReports
        patientNames = newPatientNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ai_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportsList[position]
        // Use report.id modulo to keep patient name consistent for a specific report
        val pName = if (patientNames.isNotEmpty()) {
            val safeIndex = kotlin.math.abs(report.id) % patientNames.size
            patientNames[safeIndex]
        } else {
            "Unknown Patient"
        }
        holder.bind(report, pName)
    }

    override fun getItemCount(): Int = reportsList.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExaminationType: TextView = itemView.findViewById(R.id.tvExaminationType)
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvFindingInfo: TextView = itemView.findViewById(R.id.tvFindingInfo)
        private val tvSeverityTag: TextView = itemView.findViewById(R.id.tvSeverityTag)

        fun bind(report: AiReport, pName: String) {
            tvExaminationType.text = "${report.examination_type} Scan"
            tvPatientName.text = "Patient: $pName"
            
            // Format date if possible
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = parser.parse(report.created_at)
                tvDate.text = if (date != null) formatter.format(date) else report.created_at.substringBefore("T")
            } catch (e: Exception) {
                tvDate.text = report.created_at.substringBefore("T")
            }

            tvFindingInfo.text = "Detected: ${report.finding_name}"

            // Setup severity tag colors
            tvSeverityTag.text = report.severity
            val context = itemView.context
            when (report.severity.lowercase()) {
                "high" -> {
                    tvSeverityTag.setBackgroundResource(R.drawable.bg_fff5f5_rounded)
                    tvSeverityTag.setTextColor(ContextCompat.getColor(context, R.color.critical_red))
                }
                "moderate" -> {
                    tvSeverityTag.setBackgroundResource(R.drawable.bg_fff3e0_rounded)
                    tvSeverityTag.setTextColor(ContextCompat.getColor(context, R.color.high_orange))
                }
                else -> {
                    tvSeverityTag.setBackgroundResource(R.drawable.bg_fff8e1_rounded)
                    tvSeverityTag.setTextColor(ContextCompat.getColor(context, R.color.high_orange))
                }
            }

            itemView.setOnClickListener {
                onReportClick(report)
            }
        }
    }
}
