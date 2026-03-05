package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AiChatActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var etInput: android.widget.EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_chat)

        // Handle Window Insets for Keyboard and System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Apply padding to avoid overlapping with system bars (top) and keyboard (bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, ime.bottom)
            
            insets
        }
        
        // Setup Chat
        recyclerView = findViewById(R.id.chatRecyclerView)
        etInput = findViewById(R.id.etInput)
        val btnSend = findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnSend)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter

        // Initial Greeting
        if (messages.isEmpty()) {
            addAiMessage("Hello Dr. Smith! I am your AI Clinical Assistant specialized in CT, MRI, and X-ray analysis. How can I assist you with your imaging reports today?")
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                etInput.text.clear()
                simulateAiResponse(text)
            }
        }

        // Fix: Explicitly link back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            handleBackNavigation()
        }

        setupBottomNavigation()
        updateBottomNavSelection()
    }

    private fun handleBackNavigation() {
        // Go back to Home by default when exiting chat
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // Override system back button
    override fun onBackPressed() {
        handleBackNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navPatients).setOnClickListener {
            startActivity(Intent(this, PatientsActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navAiChat).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, FollowUpActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun updateBottomNavSelection() {
        val pink = ContextCompat.getColor(this, R.color.nav_ai_chat_pink)
        val unselectedColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)

        findViewById<TextView>(R.id.tvAiChatLabel).setTextColor(pink)

        findViewById<ImageView>(R.id.ivHome).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvHome).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivPatients).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvPatients).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivSchedule).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvSchedule).setTextColor(unselectedColor)
        
        findViewById<ImageView>(R.id.ivProfile).setColorFilter(unselectedColor)
        findViewById<TextView>(R.id.tvProfile).setTextColor(unselectedColor)
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text = text, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun addAiMessage(text: String) {
        messages.add(ChatMessage(text = text, isUser = false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun simulateAiResponse(query: String) {
        val q = query.lowercase()
        val scanKeywords = listOf("ct", "mri", "x-ray", "xray", "scan", "imaging", "radiology", "modalities")
        val isScanRelated = scanKeywords.any { q.contains(it) }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isScanRelated) {
                val response = generateMedicalResponse(q)
                addAiMessage(response)
            } else {
                addAiMessage("I am specialized ONLY in CT, MRI, and X-ray scan analysis. Please ask me questions related to these imaging modalities for clinical assistance.")
            }
        }, 1000)
    }

    private fun generateMedicalResponse(query: String): String {
        return when {
            query.contains("ct") -> "The latest CT Chest analysis shows a 6mm indeterminate nodule in the right upper lobe. Standard protocol suggests follow-up imaging in 6 months to assess stability."
            query.contains("mri") -> "MRI Brain findings indicate hyperintense signal on T2-weighted images in the occipital region. This may suggest an underlying vascular pathology; radiologist review is recommended."
            query.contains("x-ray") || query.contains("xray") -> "The X-ray analysis reveals increased opacification in the right lower lobe, which suggests possible consolidation. This finding is consistent with early-stage pneumonia."
            query.contains("scan") || query.contains("imaging") -> "I can assist with detailed analysis for CT, MRI, and X-ray scans. Please specify which scan type or finding you would like to discuss."
            else -> "I am trained to analyze CT, MRI, and X-ray findings. Could you provide the specific modality or scan ID you are inquiring about?"
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }
}
