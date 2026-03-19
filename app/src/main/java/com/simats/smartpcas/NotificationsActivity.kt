package com.simats.smartpcas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var llEmptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        recyclerView = findViewById(R.id.rvNotifications)
        llEmptyState = findViewById(R.id.llEmptyState)
        
        setupRecyclerView()
        
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun fetchNotifications() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        lifecycleScope.launch {
            try {
                // Fetch AI reports and convert them to notifications
                val response = ApiClient.apiService.getAiReports(userId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val reports = response.body()?.reports ?: emptyList()
                    val notifications = reports.map { report ->
                        NotificationItem(
                            title = "Scan Result: ${report.examination_type}",
                            message = "${report.finding_name} detected with ${report.confidence_score}% confidence.",
                            timestamp = report.created_at,
                            isCritical = report.severity.lowercase() == "high" || report.severity.lowercase() == "critical"
                        )
                    }.sortedByDescending { it.timestamp }

                    if (notifications.isEmpty()) {
                        llEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        llEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.setNotifications(notifications)
                    }
                } else {
                    llEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                llEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        }
    }

    data class NotificationItem(
        val title: String,
        val message: String,
        val timestamp: String,
        val isCritical: Boolean
    )

    inner class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
        private var notifications = listOf<NotificationItem>()

        fun setNotifications(newList: List<NotificationItem>) {
            notifications = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = notifications[position]
            holder.tvTitle.text = item.title
            holder.tvMessage.text = item.message
            holder.tvTime.text = getRelativeTime(item.timestamp)
            holder.vStatusDot.visibility = if (item.isCritical) View.VISIBLE else View.INVISIBLE
        }

        override fun getItemCount() = notifications.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvMessage: TextView = view.findViewById(R.id.tvMessage)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val vStatusDot: View = view.findViewById(R.id.vStatusDot)
        }

        private fun getRelativeTime(timestamp: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(timestamp.split("T")[0]) ?: return timestamp
                val now = Calendar.getInstance().time
                val diffInMillis = now.time - date.time
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                when {
                    days == 0L -> "Today"
                    days == 1L -> "Yesterday"
                    else -> "$days days ago"
                }
            } catch (e: Exception) {
                timestamp
            }
        }
    }
}
