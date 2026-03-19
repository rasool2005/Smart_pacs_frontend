package com.simats.smartpcas

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val onQueryClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) {
            holder.bind(message)
        } else if (holder is AiViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
        }
    }

    inner class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            
            // Check if there are queries to display
            val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroupQueries)
            if (chipGroup != null) {
                chipGroup.removeAllViews()
                if (message.isQueries && message.queries != null) {
                    chipGroup.visibility = View.VISIBLE
                    for (query in message.queries) {
                        val chip = Chip(itemView.context).apply {
                            text = query
                            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_blue))
                            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(context, R.color.light_blue_bg)
                            )
                            chipStrokeColor = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(context, R.color.brand_blue)
                            )
                            chipStrokeWidth = 2f
                            setOnClickListener { onQueryClick(query) }
                        }
                        chipGroup.addView(chip)
                    }
                } else {
                    chipGroup.visibility = View.GONE
                }
            }
        }
    }
}
