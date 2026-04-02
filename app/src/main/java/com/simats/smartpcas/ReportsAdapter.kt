package com.simats.smartpcas

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsAdapter(
    private var reportsList: List<AiReport>,
    private var patientNames: List<String>,
    private val onDeleteClick: ((AiReport) -> Unit)? = null,
    private val onReportClick: (AiReport) -> Unit
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    fun updateData(newReports: List<AiReport>, newPatientNames: List<String>) {
        reportsList = newReports
        patientNames = newPatientNames
        notifyDataSetChanged()
    }

    fun removeItem(report: AiReport) {
        val position = reportsList.indexOf(report)
        if (position != -1) {
            val mutableList = reportsList.toMutableList()
            mutableList.removeAt(position)
            reportsList = mutableList
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ai_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportsList[position]
        holder.bind(report)
    }

    override fun getItemCount(): Int = reportsList.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvExaminationType: TextView = itemView.findViewById(R.id.tvExaminationType)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

        fun bind(report: AiReport) {
            // ✅ Extract Patient Name from Impression if present
            val imp = report.impression ?: ""
            var patientName = if (imp.startsWith("[Patient: ")) {
                imp.substringAfter("[Patient: ").substringBefore("]")
            } else {
                "Unknown Patient"
            }
            
            // Handle if the extracted name is literally "null" or blank
            if (patientName == "null" || patientName.isBlank()) {
                patientName = "Unknown Patient"
            }
            
            tvPatientName.text = patientName

            val examType = report.examination_type ?: "AI"
            tvExaminationType.text = if (examType.lowercase().contains("scan")) examType else "$examType Scan"
            
            var dateStr = report.created_at?.substringBefore("T") ?: "Unknown Date"
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = parser.parse(report.created_at!!)
                if (date != null) dateStr = formatter.format(date)
            } catch (e: Exception) {}

            val finding = report.finding_name ?: "Normal"
            tvSubtitle.text = "$dateStr • $finding"

            // ✅ Load the actual scanned image if available
            if (!report.image_uri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(Uri.parse(report.image_uri))
                    .placeholder(R.drawable.ic_pulse_purple)
                    .error(getFallbackIcon(examType))
                    .centerCrop()
                    .into(ivIcon)
            } else {
                ivIcon.setImageResource(getFallbackIcon(examType))
            }

            ivDelete.visibility = if (onDeleteClick != null) View.VISIBLE else View.GONE
            ivDelete.setOnClickListener { onDeleteClick?.invoke(report) }
            itemView.setOnClickListener { onReportClick(report) }
        }

        private fun getFallbackIcon(type: String): Int {
            return when (type.lowercase()) {
                "ct scan", "ct" -> R.drawable.real_ct_scan
                "mri", "mri brain" -> R.drawable.real_mri_scan
                "x-ray", "xray" -> R.drawable.real_xray_chest
                else -> R.drawable.ic_pulse_purple
            }
        }
    }
}
