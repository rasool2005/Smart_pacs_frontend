package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class StudiesAdapter(
    private var studies: List<Study>,
    private val onStudyClick: (Study) -> Unit,
    private val onDeleteClick: (Study) -> Unit
) : RecyclerView.Adapter<StudiesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvStudyTitle1)
        val tvPatient: TextView = view.findViewById(R.id.tvPatientInfo1)
        val tvDate: TextView = view.findViewById(R.id.tvDate1)
        val tvStatus: TextView = view.findViewById(R.id.tvTag)
        val ivScan: ImageView = view.findViewById(R.id.ivStudyImage)
        val btnDelete: View = view.findViewById(R.id.btnDeleteStudy1)
        val llResult: View = view.findViewById(R.id.llResult)
        val tvResultValue: TextView = view.findViewById(R.id.tvResultValue)
        val tvConfidenceValue: TextView = view.findViewById(R.id.tvConfidenceValue)
        val btnViewResults: TextView = view.findViewById(R.id.btnViewResults)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val study = studies[position]
        holder.tvTitle.text = if (study.is_ai) "[AI] ${study.study_type}" else study.study_type
        holder.tvPatient.text = study.patient_name
        holder.tvDate.text = "${study.study_date} ${study.study_time}"
        holder.tvStatus.visibility = View.GONE // Removed "Completed" label as requested

        // Display Result for AI studies
        holder.btnViewResults.visibility = View.VISIBLE
        
        if (study.is_ai) {
            holder.btnViewResults.text = "View Results →"
            holder.btnViewResults.setTextColor(android.graphics.Color.parseColor("#1A62FF"))
        } else {
            holder.btnViewResults.text = "View / Scan →"
            holder.btnViewResults.setTextColor(android.graphics.Color.parseColor("#666666"))
        }
        
        if (study.is_ai && !study.note.isNullOrEmpty()) {
            holder.llResult.visibility = View.VISIBLE
            holder.tvResultValue.text = study.note
            
            val confidence = study.ai_report?.confidence_score ?: 0.0
            holder.tvConfidenceValue.text = "${String.format("%.1f", confidence)}% Confidence"
            holder.tvConfidenceValue.visibility = if (confidence > 0) View.VISIBLE else View.GONE

            // Use prominent pink background for AI results (like on Home)
            holder.llResult.setBackgroundResource(R.drawable.bg_fff5f5_rounded)
        } else {
            holder.llResult.visibility = View.GONE
        }

        // Load image if available (Glide for AI scans)
        if (!study.image_uri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(android.net.Uri.parse(study.image_uri))
                .placeholder(R.drawable.img_mock_ct)
                .error(R.drawable.img_mock_ct)
                .into(holder.ivScan)
        } else {
            // Fallback to default modality image
            val type = study.study_type.lowercase()
            val fallback = when {
                type.contains("ct") -> R.drawable.real_ct_scan
                type.contains("mri") -> R.drawable.real_mri_scan
                type.contains("x-ray") || type.contains("xray") -> R.drawable.real_xray_chest
                else -> R.drawable.img_mock_ct
            }
            holder.ivScan.setImageResource(fallback)
        }
        
        holder.itemView.setOnClickListener { onStudyClick(study) }
        holder.btnViewResults.setOnClickListener { onStudyClick(study) }
        holder.btnDelete.setOnClickListener { onDeleteClick(study) }
    }

    override fun getItemCount() = studies.size

    fun updateData(newStudies: List<Study>) {
        this.studies = newStudies
        notifyDataSetChanged()
    }
}
