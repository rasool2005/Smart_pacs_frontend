package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiChatActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var etInput: android.widget.EditText
    private lateinit var chipGroupSuggestions: com.google.android.material.chip.ChipGroup

    private val xrayQueries = listOf(
        "What is the standard Chest X-Ray protocol?",
        "How to identify a Rib Fracture in X-Ray?",
        "What are common Pleural Effusion signs in X-Ray?",
        "How to assess Pneumothorax in Chest X-Ray?",
        "What are the Consolidation findings in X-Ray?",
        "How to evaluate GGO on a Chest X-Ray?",
        "How to measure Scoliosis Cobb Angle in X-Ray?",
        "What are the Osteoarthritis signs in X-Ray?",
        "How to identify a Bone Lytic Lesion on X-Ray?",
        "What are the standard X-Ray Dislocation views?",
        "What are the Osteoporosis features in X-Ray?",
        "How to identify Heart Failure on CXR?",
        "What are the Pediatric X-Ray safety guidelines?",
        "How to interpret an Abdominal X-Ray?",
        "What are the Wrist Fracture views in X-Ray?",
        "How to identify Shoulder Dislocation in X-Ray?",
        "What are the Ankle Fracture signs in X-Ray?",
        "How to perform a Spine X-Ray evaluation?",
        "What are the basics of Chest X-Ray anatomy?",
        "What are the Hip Joint features in X-Ray?"
    )

    private val ctQueries = listOf(
        "How to detect Head Hemorrhage on CT?",
        "What are the signs of Pulmonary Embolism on CT?",
        "How to identify Kidney Stones in a CT scan?",
        "What are the CT findings for Appendicitis?",
        "How to evaluate Liver Cirrhosis on CT?",
        "What is the Mass Effect in a Brain CT?",
        "How to interpret CT Hounsfield Units?",
        "What are the CT Contrast safety protocols?",
        "What are the common Abdominal CT signs?",
        "How to identify HRCT Lung patterns?",
        "How to evaluate Neck Trauma in CT?",
        "How to identify a Spine Fracture on CT?",
        "What are the CT Sinus anatomy basics?",
        "How to detect a Brain Infarct on CT?",
        "What are the CT Bone Scan findings?",
        "How to perform a Multiphasic CT Liver scan?",
        "What is the CT Windowing guide for clinicians?",
        "What is the standard CT Urogram protocol?",
        "What is the CT Radiation Dose (ALARA) principle?",
        "How to evaluate Pelvis Trauma on CT?"
    )

    private val mriQueries = listOf(
        "How to detect a Brain Stroke on MRI?",
        "What does T2 Hyperintensity indicate on MRI?",
        "How to identify a Meniscus Tear in MRI?",
        "What are the MRI signs of Cord Compression?",
        "How to evaluate White Matter changes on MRI?",
        "How to characterize a Tumor on MRI?",
        "What are the MRI Gadolinium safety guidelines?",
        "How to evaluate a Knee Ligament in MRI?",
        "How to identify a Spine Disc herniation on MRI?",
        "What are the Prostate PI-RADS criteria in MRI?",
        "How to identify a Shoulder Labrum tear on MRI?",
        "How to evaluate Liver Hemangioma on MRI?",
        "What are the Breast MRI imaging protocols?",
        "What are the basic MRI Sequence types?",
        "How to interpret MRI Diffusion ADC maps?",
        "What is the significance of Flair in MRI?",
        "What is the difference between T1 and T2 in MRI?",
        "What are the MRI Safety / Metal protocols?",
        "How to identify Myelitis signs on MRI?",
        "What are the basics of Pelvis MRI anatomy?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_chat)

        // Handle Window Insets for Keyboard and System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, ime.bottom)
            insets
        }
        
        // Setup Chat UI elements
        recyclerView = findViewById(R.id.chatRecyclerView)
        etInput = findViewById(R.id.etInput)
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions)
        val btnSend = findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnSend)

        chatAdapter = ChatAdapter(messages) { query ->
            etInput.setText(query)
            etInput.setSelection(query.length)
            etInput.requestFocus()
        }
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter


        setupSuggestions()

        // Input handles
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage(etInput.text.toString().trim())
                true
            } else false
        }
        
        etInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessage(etInput.text.toString().trim())
                true
            } else false
        }

        btnSend.setOnClickListener { sendMessage(etInput.text.toString().trim()) }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { handleBackNavigation() }

        setupBottomNavigation()
        updateBottomNavSelection()
    }

    private fun setupSuggestions(mode: String = "MAIN") {
        chipGroupSuggestions.removeAllViews()

        val mainChips = listOf("X-RAY", "CT", "MRI")

        val selectedQueries = when(mode) {
            "X-RAY" -> xrayQueries
            "CT" -> ctQueries
            "MRI" -> mriQueries
            else -> mainChips
        }

        // Add "BACK" button if in sub-menu
        if (mode != "MAIN") {
            addChip("⏪ BACK", isBack = true)
        }

        for (query in selectedQueries) {
            addChip(query, isBack = false, currentMode = mode, queriesList = selectedQueries)
        }
    }

    private fun addChip(query: String, isBack: Boolean, currentMode: String = "MAIN", queriesList: List<String>? = null) {
        val chip = com.google.android.material.chip.Chip(this).apply {
            text = query
            isClickable = true
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@AiChatActivity, R.color.white)
            )
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.brand_blue))
            setChipStrokeColorResource(R.color.brand_blue)
            chipStrokeWidth = 2f
            
            setOnClickListener {
                if (isBack) {
                    setupSuggestions("MAIN")
                } else if (currentMode == "MAIN") {
                    // Transition to sub-menu
                    setupSuggestions(query)
                    val queriesToShow = when(query) {
                        "X-RAY" -> xrayQueries
                        "CT" -> ctQueries
                        "MRI" -> mriQueries
                        else -> null
                    }
                    if (queriesToShow != null) {
                        addAiMessage("Here are 20 clinical queries about $query:", isQueries = true, queries = queriesToShow)
                    }
                } else {
                    // Fill input for sub-menu queries
                    etInput.setText(query)
                    etInput.setSelection(query.length)
                    etInput.requestFocus()
                }
            }
        }
        chipGroupSuggestions.addView(chip)
    }

    private fun sendMessage(text: String) {
        if (text.isNotEmpty()) {
            addUserMessage(text)
            etInput.text.clear()
            
            // Hide keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etInput.windowToken, 0)
            
            simulateAiResponse(text)
        }
    }

    private fun handleBackNavigation() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

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
        findViewById<LinearLayout>(R.id.navAiChat).setOnClickListener { }
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

    private fun addAiMessage(text: String, isQueries: Boolean = false, queries: List<String>? = null) {
        messages.add(ChatMessage(text = text, isUser = false, isQueries = isQueries, queries = queries))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun simulateAiResponse(query: String) {
        val typingMsg = ChatMessage(text = "...", isUser = false, isTyping = true)
        messages.add(typingMsg)
        val initialIndex = messages.size - 1
        chatAdapter.notifyItemInserted(initialIndex)
        recyclerView.smoothScrollToPosition(initialIndex)

        lifecycleScope.launch(Dispatchers.IO) {
            val lowerQuery = query.lowercase().trim()
            val isBasicDefinition = lowerQuery.contains("what is x-ray") || 
                                  lowerQuery.contains("what is ct") || 
                                  lowerQuery.contains("what is mri")

            val localResponse = generateLocalResponse(query)
            var apiResponse: String? = null
            
            if (isBasicDefinition) {
                // Priority 1: Use our custom high-quality definitions for basic modalities
                apiResponse = localResponse
            } else {
                // Priority 2: Try Server for professional report structure
                try {
                    val result = ApiClient.apiService.aiChat(query)
                    if (result.isSuccessful && result.body() != null) {
                        val serverResp = result.body()?.response
                        // If server response is too short or generic, we check if local is better
                        if (!serverResp.isNullOrBlank() && 
                            serverResp.length > 60 && 
                            !serverResp.contains("not identified", ignoreCase = true)) {
                            apiResponse = serverResp
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AiChat", "Server Call Failed: ${e.message}")
                }
            }

            // Fallback to locally mapped expert answer if server gave a generic/repetitive response
            val finalResponse = if (apiResponse.isNullOrBlank()) localResponse else apiResponse

            withContext(Dispatchers.Main) {
                val currentIndex = messages.indexOf(typingMsg)
                if (currentIndex != -1) {
                    messages.removeAt(currentIndex)
                    chatAdapter.notifyItemRemoved(currentIndex)
                }
                addAiMessage(finalResponse)
            }
        }
    }

    private fun generateLocalResponse(userMessage: String): String {
        val cleanInput = userMessage.lowercase().trim().replace(Regex("[?!.,]"), "")
        val tokens = cleanInput.split(Regex("\\s+")).filter { it.length > 2 }
        
        val greetings = listOf("hi", "hello", "hey", "greetings", "morning")
        if (greetings.any { cleanInput.contains(it) }) {
            return "Hello! I am the SMRT PACS Clinical Assistant. How can I assist you with your radiological review today?"
        }

        if (cleanInput.contains("thank")) {
            return "You're welcome! I'm here to help. Do you have any other questions?"
        }

        val coreMedicalTerms = MEDICAL_TERMS.filter { it !in listOf("hello", "hi", "hey", "help", "thank") }
        if (coreMedicalTerms.none { cleanInput.contains(it) } && tokens.isNotEmpty()) {
            return "I am specialized in radiology. Please ask about X-ray, CT, or MRI findings."
        }
        
        var bestMatch: KBEntry? = null
        var maxScore = 0
        for (entry in knowledgeBase) {
            var score = 0
            for (keyword in entry.keywords) {
                val kw = keyword.lowercase()
                if (cleanInput == kw) score += 100 
                else if (cleanInput.contains(kw)) score += kw.length * 2
                val kwTokens = kw.split(Regex("\\s+")).filter { it.length > 2 }
                for (t in tokens) if (kwTokens.contains(t)) score += t.length
            }
            if (score > maxScore) {
                maxScore = score
                bestMatch = entry
            }
        }
        
        val disclaimer = "\n\nNote: This is an AI assisted interpretation."
        if (bestMatch != null && maxScore > 2) return bestMatch.response + disclaimer
        
        val genericResponses = listOf(
            "Specific imaging features for this query are not identified. Consider clinical correlation.",
            "I recommend reviewing the 'Impression' section of the report for this specific case.",
            "Generally, for soft tissue detail, MRI is preferred over CT for this region.",
            "Ensure the study was performed with the correct protocol before finalizing the interpretation."
        )
        return genericResponses.random() + disclaimer
    }

    data class KBEntry(val keywords: List<String>, val response: String)

    companion object {
        val MEDICAL_TERMS = listOf("x-ray", "xray", "mri", "ct scan", "ct", "dicom", "pacs", "contrast", "fracture", "pneumonia", "tumor", "stroke")
        
        val knowledgeBase = listOf(
            // General Knowledge
            KBEntry(listOf("what is x-ray", "description x-ray", "xray"), "X-ray is a basic imaging technique that uses radiation to capture images of the body.\n\nBest for: Bones.\nShows: Fractures, infections, and lung issues.\nAdvantage: Fast and low cost.\nExample: Detecting a bone fracture."),
            KBEntry(listOf("what is ct scan", "what is ct", "description ct"), "CT (Computed Tomography) uses multiple X-ray images to create detailed cross-sectional images.\n\nBest for: Internal organs, brain, and lungs.\nAdvantage: More detailed than a standard X-ray.\nShows: Tumors, bleeding, and internal injuries.\nExample: Detecting brain hemorrhage."),
            KBEntry(listOf("what is mri", "description mri", "magnetic resonance"), "MRI (Magnetic Resonance Imaging) uses strong magnetic fields and radio waves (no radiation).\n\nBest for: Soft tissues.\nShows: Brain, muscles, and ligaments.\nAdvantage: Very high detail for soft tissue.\nExample: Detecting a ligament tear or brain tumor."),
            
            // X-RAY Knowledge
            KBEntry(listOf("chest x-ray protocol", "cxr protocol"), "Standard CXR includes PA and Lateral views. Paired with clinical history, it evaluates lung parenchyma and cardiac silhouette."),
            KBEntry(listOf("rib fracture", "broken rib"), "Look for cortical disruption on oblique views. CT may be required for diagnosing non-displaced or cartilaginous fractures."),
            KBEntry(listOf("pleural effusion x-ray"), "Features include blunting of the costophrenic angles and meniscus sign. A lateral decubitus view can confirm small effusions."),
            KBEntry(listOf("pneumothorax check", "lung collapse"), "Identify a thin, peripheral visceral pleural line with no lung markings beyond it. Expiratory films can enhance findings."),
            KBEntry(listOf("consolidation x-ray", "pneumonia"), "Appears as increased lung density obscuring pulmonary vessels, often with air bronchograms, typical of pneumonia or hemorrhage."),
            KBEntry(listOf("ggo assessment", "ground glass"), "Hazy area of increased lung opacity that doesn't obscure vascular markings. Can represent early infection, edema, or inflammation."),
            KBEntry(listOf("scoliosis measurement", "cobb angle"), "Measure Cobb angle between the most tilted vertebrae in the curve. >10° defines scoliosis; >40° may require surgical review."),
            KBEntry(listOf("osteoarthritis signs", "joint space"), "Classic signs: joint space narrowing, subchondral sclerosis, marginal osteophytes, and subchondral cysts."),
            KBEntry(listOf("bone lytic lesion", "bone lesion"), "Focal bone destruction. Assess margins: well-defined (geographic), moth-eaten, or permeative to determine aggressiveness."),
            KBEntry(listOf("dislocation views", "shoulder dislocation"), "Order orthogonal views (e.g., AP and Axillary/Y-view for shoulder) to confirm the direction (anterior vs posterior)."),
            KBEntry(listOf("osteoporosis features"), "Thinning of the cortex and reduced trabecular density. CXR is not as sensitive as DEXA but can show vertebral wedge-fractures."),
            KBEntry(listOf("heart failure cxr"), "Look for cardiomegaly, Kerley B lines, cephalization of vessels, and pleural effusions (Cephalization indicates elevated LAP)."),
            KBEntry(listOf("pediatric x-ray safety"), "Use low-dose (ALARA) protocols. Gonadal shielding is standard unless it obscures the anatomy under review."),
            KBEntry(listOf("abdominal x-ray interpretation", "axr"), "Assess for ileus, obstructions (dilated loops), or pneumoperitoneum (Rigler's sign). Check calcifications (kidney stones, biliary)."),
            KBEntry(listOf("wrist fracture view"), "Standard views are AP, Lateral, and Oblique. A dedicated scaphoid view is required if scaphoid pain is clinically noted."),
            KBEntry(listOf("spine x-ray evaluation"), "Assess vertebral alignment, disc space height, and look for pars defects or osteophytic changes."),
            KBEntry(listOf("chest radiograph anatomy"), "PA film evaluates: Heart, Lungs, Pleura, Hila, Mediastinum, and Diaphragm. Always check the blind zones (retrocardiac, apices)."),
            
            // CT Knowledge
            KBEntry(listOf("head hemorrhage ct", "brain bleed"), "Look for hyperdense (bright) acute blood: biconvex (EDH), crescent (SDH), or intraventricular blood."),
            KBEntry(listOf("pulmonary embolism ct", "pe scan"), "CTPA shows intraluminal filling defects in pulmonary arteries. Check for right heart strain signs (RV/LV ratio >1)."),
            KBEntry(listOf("kidney stones ct", "ncct kub"), "Non-contrast CT is the gold standard for urolithiasis. Hyperdense foci in the ureter or renal pelvis confirm the diagnosis."),
            KBEntry(listOf("appendicitis check ct"), "Appendix diameter >6mm, wall thickening, and 'periapendiceal fat stranding' are key diagnostic features on CT."),
            KBEntry(listOf("liver cirrhosis ct"), "Nodular surface contour, caudate lobe hypertrophy, and splenomegaly. Check for varices indicating portal hypertension."),
            KBEntry(listOf("mass effect brain", "midline shift"), "Displacement of brain structures (midline shift, ventricle compression) due to hematoma, tumor, or edema."),
            KBEntry(listOf("ct hounsfield units", "hu values"), "HU values: Water=0, Bone=+1000, Air=-1000, Fat=-50 to -100. Helps characterize masses (e.g., fat indicates lipoma)."),
            KBEntry(listOf("ct contrast safety", "gfr"), "Check serum creatinine/eGFR. If eGFR <30, contrast is generally avoided due to Acute Kidney Injury (AKI) risk."),
            KBEntry(listOf("abdominal ct signs"), "Look for bowel wall thickening, mass lesions, and pneumoperitoneum. Assess retroperitoneal lymphadenopathy."),
            KBEntry(listOf("hrct lung pattern"), "Identify septal thickening, cystic changes, or honeycombing (UIP/NSIP patterns) for interstitial lung disease (ILD) diagnosis."),
            KBEntry(listOf("neck trauma ct"), "Emergency CT assesses cervical spine alignment, hardware, and soft tissue for potential injuries or hematoma."),
            KBEntry(listOf("spine fracture ct"), "CT is superior for bony detail. Identify compression, burst, or facet fractures. Check for spinal canal narrowing."),
            KBEntry(listOf("ct sinus anatomy"), "Assess paranasal sinus drainage pathways, septal deviation, and mucosal thickening indicative of sinusitis or polyps."),
            KBEntry(listOf("brain infarct ct", "stroke ct"), "Early signs: 'hyperdense MCA sign', loss of gray-white differentiation, and sulcal effacement (may be normal in very early stroke)."),
            KBEntry(listOf("multiphasic ct liver"), "Essential for HCC detection. Assess arterial phase (enhancement), portal venous, and washout phases."),
            KBEntry(listOf("ct radiation dose", "alara"), "ALARA principle (As Low As Reasonably Achievable). Use optimized protocols to minimize life-long stochastic risk."),

            // MRI Knowledge
            KBEntry(listOf("brain stroke mri", "dwi adc"), "DWI/ADC restriction is highly sensitive for acute ischemia. Lesions are bright on DWI and dark on ADC mapping."),
            KBEntry(listOf("t2 hyperintensity"), "Bright signal on T2 indicates fluid, edema, or inflammation. Common in MS plaques, tumors, and ischemic injury."),
            KBEntry(listOf("meniscus tear mri", "knee injury"), "Grade 3 signal reaching the articular surface on T2/PD sequences diagnostic for a tear."),
            KBEntry(listOf("cord compression mri"), "Spinal cord indentation with T2 hyperintensity within the cord (myelomalacia) requires urgent neurosurgical review."),
            KBEntry(listOf("white matter change"), "Nonspecific hyperintensities common in aging/vascular disease. Dawson's fingers are specific to Multiple Sclerosis."),
            KBEntry(listOf("tumor character mri"), "Assess enhancement, edema, and necrosis. Perfusion MRI can help differentiate radiation necrosis from recurrence."),
            KBEntry(listOf("gadolinium safety"), "Avoid in severe renal impairment (eGFR <30) due to NSF risk. Newer macrocyclic agents carry lower risk."),
            KBEntry(listOf("knee ligament mri", "acl tear"), "Check ACL/PCL continuity. ACL tears are characterized by non-visualization or wavy appearance on sagittal PD views."),
            KBEntry(listOf("spine disc herniation", "mri"), "Differentiate between protrusion (base wider than herniation) and extrusion (neck narrower than herniation)."),
            KBEntry(listOf("prostate pi-rads"), "PI-RADS v2.1 categorizes risk from 1 to 5. PI-RADS 4/5 indicates high suspicion and usually requires biopsy."),
            KBEntry(listOf("shoulder labrum mri"), "Look for Labrum signal intensity changes or detachment. SLAP lesions involve the superior labrum/biceps anchor."),
            KBEntry(listOf("liver hemangioma mri"), "Classic 'lightbulb' bright signal on T2 and peripheral nodular discontinuous enhancement on post-gadolinium sequences."),
            KBEntry(listOf("mri sequence basics", "t1 vs t2"), "T1: Fat is bright, Fluid is dark (best for anatomy). T2: Fluid is bright (best for pathology). FLAIR: T2 with fluid suppressed."),
            KBEntry(listOf("diffusion adc maps"), "DWI restriction suggests high cellularity (tumor) or cytotoxic edema (stroke). ADC values help confirm true restriction."),
            KBEntry(listOf("mri flair significance"), "FLAIR suppresses CSF signal, making periventricular white matter lesions much more visible than standard T2."),
            KBEntry(listOf("mri safety metal"), "Strict screening for pacemakers, metal fragments, or non-MRI compatible implants due to strong magnetic fields."),
            KBEntry(listOf("myelitis signs mri"), "Inflammation of the cord showing T2 hyperintensity extending over several segments. Check for peripheral enhancement."),
            KBEntry(listOf("pelvis mri anatomy"), "High resolution T2 sequences are essential to evaluate the prostate, uterus, ovaries, and rectum for staging.")
        )
    }
}
