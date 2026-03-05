package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PacsAdapter(
    private val studies: List<PacsStudy>,
    private val onStudyClick: (PacsStudy) -> Unit
) : RecyclerView.Adapter<PacsAdapter.PacsViewHolder>() {

    class PacsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivStudyImage: ImageView = itemView.findViewById(R.id.ivStudyImage)
        val ivModalityIcon: ImageView = itemView.findViewById(R.id.ivModalityIcon)
        val tvModality: TextView = itemView.findViewById(R.id.tvModality)
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pacs_study, parent, false)
        return PacsViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacsViewHolder, position: Int) {
        val study = studies[position]

        holder.tvPatientName.text = study.patientName
        holder.tvModality.text = study.modality
        holder.tvDate.text = study.date

        // Mock Logic for Icons & Images based on Modality
        when (study.modality) {
            "X-Ray" -> {
                holder.ivStudyImage.setImageResource(R.drawable.img_mock_xray)
            }
            "CT Scan" -> {
                holder.ivStudyImage.setImageResource(R.drawable.img_mock_ct)
            }
            "MRI" -> {
                holder.ivStudyImage.setImageResource(R.drawable.real_mri) // Replaced icon with real scan
            }
        }

        holder.itemView.setOnClickListener {
            onStudyClick(study)
        }
    }

    override fun getItemCount() = studies.size
}
