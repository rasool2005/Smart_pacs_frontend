package com.simats.smartpcas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class OnboardingItem(
    val title: String,
    val description: String,
    val iconRes: Int
)

class OnboardingAdapter(private val onboardingItems: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_onboarding,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int = onboardingItems.size

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon = view.findViewById<ImageView>(R.id.ivOnboardingIcon)
        private val tvTitle = view.findViewById<TextView>(R.id.tvOnboardingTitle)
        private val tvDescription = view.findViewById<TextView>(R.id.tvOnboardingDescription)

        fun bind(onboardingItem: OnboardingItem) {
            tvTitle.text = onboardingItem.title
            tvDescription.text = onboardingItem.description
            ivIcon.setImageResource(onboardingItem.iconRes)
        }
    }
}
