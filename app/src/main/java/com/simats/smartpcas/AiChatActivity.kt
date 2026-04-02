package com.simats.smartpcas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiChatActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var chipGroupSuggestions: ChipGroup

    private val xrayQueries = listOf(
        "Lung opacity chest x-ray",
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
        "How to evaluate Pelvis Trauma on CT?",
        "How to identify common Abdominal CT signs?"
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, ime.bottom)
            insets
        }
        
        recyclerView = findViewById(R.id.chatRecyclerView)
        etInput = findViewById(R.id.etInput)
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions)
        val btnSend = findViewById<MaterialCardView>(R.id.btnSend)

        chatAdapter = ChatAdapter(messages) { query ->
            etInput.setText(query)
            etInput.setSelection(query.length)
            etInput.requestFocus()
        }
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter

        setupSuggestions()

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
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
        if (mode != "MAIN") {
            addChip("⏪ BACK", isBack = true)
        }
        for (query in selectedQueries) {
            addChip(query, isBack = false, currentMode = mode)
        }
    }

    private fun addChip(query: String, isBack: Boolean, currentMode: String = "MAIN") {
        val chip = Chip(this).apply {
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
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            val localResponse = generateLocalResponse(query)
            var finalResponse = localResponse
            
            val isGeneric = localResponse.contains("Specific imaging features", ignoreCase = true) ||
                            localResponse.contains("specialized in diagnostic radiology", ignoreCase = true)

            if (isGeneric) {
                try {
                    val result = ApiClient.apiService.aiChat(query)
                    if (result.isSuccessful && result.body() != null) {
                        val serverResp = result.body()?.response
                        if (!serverResp.isNullOrBlank() && serverResp.length > 20) {
                            finalResponse = serverResp
                        }
                    }
                } catch (e: Exception) { }
            } else {
                kotlinx.coroutines.delay(800)
            }

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
        
        if (bestMatch != null && maxScore > 3) return bestMatch.response
        
        val coreMedicalTerms = MEDICAL_TERMS.filter { it !in listOf("hello", "hi", "hey", "help", "thank") }
        if (coreMedicalTerms.none { cleanInput.contains(it) } && tokens.isNotEmpty()) {
            return "I am specialized in diagnostic radiology. Please ask about X-ray, CT, or MRI protocols and findings."
        }
        
        return "1. Observations:\nSpecific imaging features for this query require further clinical context.\n\n2. Possible Findings:\nFindings may vary based on modality and anatomical region.\n\n3. Differential Diagnosis:\nBroad categories including inflammatory, neoplastic, or vascular etiologies.\n\n4. Recommendation:\nFurther evaluation with targeted imaging or clinical correlation is suggested.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."
    }

    data class KBEntry(val keywords: List<String>, val response: String)

    companion object {
        val MEDICAL_TERMS = listOf("x-ray", "xray", "mri", "ct", "dicom", "pacs", "contrast", "fracture", "pneumonia", "tumor", "stroke", "hemorrhage", "lesion", "tear", "scan")
        
        val knowledgeBase = listOf(
            KBEntry(listOf("lung opacity chest x-ray", "lung opacity", "chest x-ray"), "1. Observations:\nIncreased opacity in the lung field.\n\n2. Possible Findings:\nMay indicate infection, fluid accumulation, or inflammation.\n\n3. Differential Diagnosis:\n- Pneumonia\n- Pulmonary edema\n- Atelectasis\n\n4. Recommendation:\nClinical correlation and further imaging may be required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("fracture x-ray", "bone fracture", "broken bone"), "1. Observations:\nDiscontinuity or break in bone cortex with a lucent fracture line.\n\n2. Possible Findings:\nIndicates bone fracture or crack with possible displacement.\n\n3. Differential Diagnosis:\n- Acute fracture\n- Stress fracture\n- Pathological fracture\n\n4. Recommendation:\nOrthopedic evaluation and possible immobilization advised.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("brain hemorrhage ct", "head hemorrhage", "brain bleed"), "1. Observations:\nHyperdense (bright) region within the brain parenchyma on non-contrast CT.\n\n2. Possible Findings:\nSuggestive of acute intracranial bleeding.\n\n3. Differential Diagnosis:\n- Intracerebral hemorrhage\n- Subdural hematoma\n- Epidural hematoma\n\n4. Recommendation:\nImmediate clinical evaluation and urgent neurosurgical review required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("hypodense lesion ct", "hypodense lesion"), "1. Observations:\nArea of lower density (dark) compared to surrounding brain tissue on CT.\n\n2. Possible Findings:\nMay indicate fluid accumulation, cyst, or ischemic tissue damage.\n\n3. Differential Diagnosis:\n- Infarction (ischemic stroke)\n- Arachnoid or epidermoid cyst\n- Necrotic tumor\n\n4. Recommendation:\nFurther imaging with contrast or MRI diffusion sequences may clarify the nature.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("hyperintensity mri", "t2 hyperintensity", "mri brightness"), "1. Observations:\nBright (hyperintense) signal area on T2 or FLAIR weighted MRI sequences.\n\n2. Possible Findings:\nIndicates increased water content, inflammation, or abnormal tissue.\n\n3. Differential Diagnosis:\n- Vasogenic edema\n- Demyelination (Multiple Sclerosis)\n- Tumor or metastasis\n\n4. Recommendation:\nCorrelation with DWI to exclude acute infarction; contrast-enhanced MRI if mass lesion suspected.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ligament tear mri", "ligament injury", "acl tear"), "1. Observations:\nDisruption or signal discontinuity within the ligament on T2-weighted MRI.\n\n2. Possible Findings:\nSuggests partial or complete ligament injury or tear.\n\n3. Differential Diagnosis:\n- Partial tear\n- Complete tear\n- Mucoid degeneration / sprain\n\n4. Recommendation:\nOrthopedic consultation recommended; clinical stability tests (e.g., Lachman for ACL).\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("pleural effusion x-ray", "fluid in lung", "effusion signs"), "1. Observations:\nBlunting of the costophrenic angle; meniscus sign; fluid-level opacity.\n\n2. Possible Findings:\nFluid accumulation in the pleural space.\n\n3. Differential Diagnosis:\n- Transudative effusion (CHF, cirrhosis)\n- Exudative effusion (pneumonia, malignancy)\n- Hemothorax\n\n4. Recommendation:\nLateral decubitus view or ultrasound for volume quantification and assessment.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("kidney stones ct", "renal calculi", "urolithiasis"), "1. Observations:\nHyperdense (bright) structures in the renal pelvis, ureter, or bladder on non-contrast CT.\n\n2. Possible Findings:\nIndicates presence of urinary calculi causing potential obstruction.\n\n3. Differential Diagnosis:\n- Renal calculi (calcium oxalate, uric acid)\n- Ureteric stones\n- Phleboliths (pelvic veins)\n\n4. Recommendation:\nUrological consultation advised; assess for hydronephrosis and degree of obstruction.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("brain tumor mri", "intracranial neoplasm", "brain mass"), "1. Observations:\nAbnormal mass with altered signal intensity on T T1, T2, and FLAIR MRI sequences.\n\n2. Possible Findings:\nSuggests presence of intracranial neoplasm with possible surrounding edema.\n\n3. Differential Diagnosis:\n- Primary glioma (GBM, astrocytoma)\n- Brain metastasis\n- Meningioma\n\n4. Recommendation:\nContrast-enhanced MRI for further characterization; neurosurgical and oncology referral.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("stroke ct findings", "ischemic stroke ct", "hemorrhagic stroke ct"), "1. Observations:\nHypodense (dark) or hyperdense (bright) regions in brain territory on non-contrast CT.\n\n2. Possible Findings:\nIndicates ischemic or hemorrhagic stroke depending on density pattern.\n\n3. Differential Diagnosis:\n- Ischemic stroke (hypodense)\n- Hemorrhagic stroke (hyperdense)\n- Hyperdense MCA sign (dense clot)\n\n4. Recommendation:\nImmediate medical attention required; CTA for vessel occlusion assessment (thrombectomy planning).\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("pneumothorax x-ray", "collapsed lung"), "1. Observations:\nVisible visceral pleural line with absence of lung markings peripheral to it.\n\n2. Possible Findings:\nIndicates air in the pleural space causing lung collapse.\n\n3. Differential Diagnosis:\n- Simple spontaneous pneumothorax\n- Traumatic pneumothorax\n- Tension pneumothorax (tracheal shift present)\n\n4. Recommendation:\nCheck for tracheal deviation (tension); immediate clinical attention required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("osteoporosis x-ray", "low bone density"), "1. Observations:\nReduced bone density with thinning of the cortical bone and trabeculae.\n\n2. Possible Findings:\nIndicates weakened bone mineral density with fracture risk.\n\n3. Differential Diagnosis:\n- Osteoporosis\n- Osteopenia\n- Hyperparathyroidism\n\n4. Recommendation:\nBone density test (DEXA scan) recommended; endocrinology referral.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("pulmonary embolism ct", "pe scan", "ctpa"), "1. Observations:\nIntraluminal filling defects in the pulmonary arteries on CTPA.\n\n2. Possible Findings:\nIndicates blockage of pulmonary blood flow by thrombus.\n\n3. Differential Diagnosis:\n- Acute pulmonary embolism\n- Chronic thromboembolic disease\n- Tumor embolus\n\n4. Recommendation:\nUrgent anticoagulation assessment; evaluate for right heart strain markers on CT.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("appendicitis ct scan", "appendix inflammation"), "1. Observations:\nEnlarged appendix (>6mm diameter) with wall thickening and periappendiceal fat stranding.\n\n2. Possible Findings:\nSuggests acute inflammation of the appendix.\n\n3. Differential Diagnosis:\n- Acute appendicitis\n- Periappendiceal abscess\n- Mesenteric adenitis\n\n4. Recommendation:\nSurgical consultation recommended; correlate with Alvarado score.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("stroke mri findings", "dwi restricted", "acute stroke mri"), "1. Observations:\nDiffusion restriction (bright DWI + dark ADC) in the affected brain territory.\n\n2. Possible Findings:\nIndicates acute ischemic stroke with cytotoxic edema.\n\n3. Differential Diagnosis:\n- Acute ischemic stroke\n- Transient ischemic attack (TIA)\n- Hypoglycemia (mimics stroke on MRI)\n\n4. Recommendation:\nImmediate neurological evaluation; CTA for large vessel occlusion assessment.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("meniscus tear mri", "knee injury mri"), "1. Observations:\nAbnormal T2 signal reaching the articular surface of the meniscus on knee MRI.\n\n2. Possible Findings:\nSuggests meniscal tear or degeneration (Grade 3 signal).\n\n3. Differential Diagnosis:\n- Medial or lateral meniscus tear\n- Bucket-handle tear (if displaced fragment)\n- Degenerative meniscal changes\n\n4. Recommendation:\nOrthopedic consultation advised; consider arthroscopy if symptoms persist.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("scoliosis x-ray", "cobb angle measurement"), "1. Observations:\nLateral curvature of the spine exceeding 10 degrees on standing radiograph.\n\n2. Possible Findings:\nIndicates abnormal spinal alignment with possible rotational deformity.\n\n3. Differential Diagnosis:\n- Adolescent idiopathic scoliosis\n- Degenerative scoliosis (adult)\n- Neuromuscular scoliosis\n\n4. Recommendation:\nMeasure Cobb angle on standing full-spine radiographs; spine specialist consultation.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("liver cirrhosis ct", "nodular liver", "caudate hypertrophy"), "1. Observations:\nNodular and irregular liver surface contour; hypertrophy of caudate lobe; splenomegaly; ascites.\n\n2. Possible Findings:\nSuggests chronic liver parenchymal damage with portal hypertension.\n\n3. Differential Diagnosis:\n- Liver cirrhosis\n- Chronic hepatitis\n- Budd-Chiari syndrome\n\n4. Recommendation:\nHCC screening with multiphasic CT or MRI; hepatology consultation.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("spinal cord compression mri", "cord impingement", "myelopathy mri"), "1. Observations:\nNarrowed spinal canal with focal T2 high signal change within the spinal cord.\n\n2. Possible Findings:\nIndicates myelopathy due to external compression on the cord.\n\n3. Differential Diagnosis:\n- Disc herniation\n- Extradural tumor or metastasis\n- Spinal stenosis\n\n4. Recommendation:\nUrgent neurological consultation required if acute neurological deficit is present.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("calcification ct imaging", "hounsfield units calcification"), "1. Observations:\nFocal high-density (bright) areas with Hounsfield Units >100 HU in soft tissue.\n\n2. Possible Findings:\nIndicates calcium deposits within tissue or vessels.\n\n3. Differential Diagnosis:\n- Dystrophic calcification (chronic infection, old hematoma)\n- Metastatic calcification (hypercalcemia)\n- Tumoral calcification (e.g., meningioma, teratoma)\n\n4. Recommendation:\nClinical context is essential; correlate with serum calcium and tissue diagnosis if required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("mri", "magnetic resonance imaging", "what is mri"), "1. Observations:\nMRI uses magnetic fields and radio waves — no ionizing radiation.\n\n2. Possible Findings:\nKey sequences: T1 (anatomy), T2 (pathology/edema), FLAIR (periventricular), DWI/ADC (stroke).\n\n3. Differential Diagnosis:\nSuperior for Brain (MS, tumor), Spine (disc), and Soft tissue (ligaments).\n\n4. Recommendation:\nAlways confirm metallic implant safety before scanning.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("x-ray", "radiograph", "what is x-ray"), "1. Observations:\nX-Ray uses ionizing radiation to produce 2D projection images.\n\n2. Possible Findings:\nBone appears white (opaque), Air appears black (lucent). Best for fractures and chest screening.\n\n3. Differential Diagnosis:\nUsed for pneumonia, effusion, fractures, and alignment.\n\n4. Recommendation:\nPA view is standard for chest radiographs to minimize heart magnification.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct", "computed tomography", "what is ct"), "1. Observations:\nCT uses X-rays with computer reconstruction to produce cross-sectional images (3D).\n\n2. Possible Findings:\nMeasured in Hounsfield Units (HU). Bone is +1000, Air is -1000, Water is 0.\n\n3. Differential Diagnosis:\nPreferred for emergency trauma, hemorrhage, and lung imaging.\n\n4. Recommendation:\nAlways review in multiple windows (Lung, Bone, Soft Tissue).\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct scan contrast safety", "gfr protocols"), "Observations:\nUse of iodinated contrast in CT imaging requires patient screening.\n\nPossible Findings:\nRisk of allergic reactions or contrast-induced nephropathy.\n\nDifferential Considerations:\n- Contrast allergy\n- Renal impairment\n- Previous adverse reactions\n\nRecommendation:\nCheck renal function (creatinine), ensure hydration, and review allergy history before administration.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("hrct lung patterns", "lung fibrosis"), "Observations:\nPresence of ground-glass opacities, reticulations, or nodules.\n\nPossible Findings:\nSuggests interstitial or inflammatory lung disease.\n\nDifferential Diagnosis:\n- Interstitial lung disease\n- Pulmonary fibrosis\n- Infection\n\nRecommendation:\nCorrelation with clinical history and pulmonary function tests.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("neck trauma ct", "c-spine trauma"), "Observations:\nAssessment of cervical spine alignment and soft tissues.\n\nPossible Findings:\nFractures, soft tissue swelling, or vascular injury.\n\nDifferential Diagnosis:\n- Cervical fracture\n- Ligament injury\n- Hematoma\n\nRecommendation:\nUrgent evaluation and stabilization if trauma suspected.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("spine fracture ct", "vertebral fracture"), "Observations:\nDiscontinuity in vertebral body or alignment changes.\n\nPossible Findings:\nIndicates vertebral fracture.\n\nDifferential Diagnosis:\n- Compression fracture\n- Burst fracture\n- Pathological fracture\n\nRecommendation:\nOrthopedic or neurosurgical consultation advised.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("brain infarct ct", "stroke signs ct"), "Observations:\nHypodense area in affected brain region.\n\nPossible Findings:\nIndicates ischemic infarction.\n\nDifferential Diagnosis:\n- Ischemic stroke\n- Edema\n- Old infarct\n\nRecommendation:\nImmediate neurological assessment required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct windowing guide", "window width level"), "Observations:\nAdjustment of window width and level for tissue visualization.\n\nPossible Findings:\nEnhances visibility of soft tissue, lung, or bone structures.\n\nDifferential Considerations:\n- Lung window\n- Bone window\n- Soft tissue window\n\nRecommendation:\nUse appropriate window settings based on diagnostic requirement.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct urogram protocol", "kub ct"), "Observations:\nMultiphasic imaging including non-contrast, nephrographic, and excretory phases.\n\nPossible Findings:\nEvaluates urinary tract structures for tumors or obstruction.\n\nDifferential Diagnosis:\n- Stones\n- Tumors\n- Obstruction\n\nRecommendation:\nFollow proper contrast timing and hydration protocol.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("evaluate knee ligament mri", "acl pcl injury"), "Observations:\nContinuity and signal intensity of ligaments.\n\nPossible Findings:\nIndicates ligament injury or tear.\n\nDifferential Diagnosis:\n- ACL tear\n- PCL injury\n- Sprain\n\nRecommendation:\nOrthopedic consultation recommended.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("spine disc herniation mri", "disc protrusion"), "Observations:\nDisc protrusion compressing adjacent structures.\n\nPossible Findings:\nIndicates herniated disc.\n\nDifferential Diagnosis:\n- Disc bulge\n- Herniation\n- Degeneration\n\nRecommendation:\nClinical correlation and spine specialist consultation.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("difference t1 t2 mri", "t1 vs t2"), "Observations:\nT1 shows anatomy; T2 highlights fluid and pathology.\n\nPossible Findings:\nDifferent tissue contrasts based on sequence.\n\nDifferential Considerations:\n- T1-weighted imaging\n- T2-weighted imaging\n\nRecommendation:\nUse both sequences for comprehensive evaluation.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("interpret mri diffusion adc", "adc mapping"), "Observations:\nAreas of restricted diffusion appear bright on DWI and dark on ADC.\n\nPossible Findings:\nIndicates acute ischemia or cellular injury.\n\nDifferential Diagnosis:\n- Stroke\n- Tumor\n- Abscess\n\nRecommendation:\nCorrelate with clinical findings and other sequences.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("significance flair mri", "periventricular lesions flair"), "Observations:\nSuppresses fluid signal to highlight lesions.\n\nPossible Findings:\nImproves visibility of edema and lesions (e.g., MS plaques).\n\nDifferential Diagnosis:\n- Multiple sclerosis\n- Infarction\n- Inflammation\n\nRecommendation:\nUse in brain imaging for lesion detection.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("common abdominal ct signs", "fat stranding abdomen"), "Observations:\nAbnormal organ size, density changes, or fluid collections.\n\nPossible Findings:\nMay indicate infection, inflammation, or mass lesions.\n\nDifferential Diagnosis:\n- Appendicitis\n- Tumor\n- Abscess\n- Bowel obstruction\n\nRecommendation:\nFurther clinical and laboratory correlation advised.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct sinus anatomy basics", "ostiomeatal complex"), "Observations:\nVisualization of maxillary, ethmoid, frontal, and sphenoid sinuses.\n\nPossible Findings:\nHelps identify sinus structure and abnormalities.\n\nDifferential Diagnosis:\n- Sinusitis\n- Polyps\n- Mucosal thickening\n\nRecommendation:\nEvaluate sinus symmetry and air-fluid levels.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct bone scan findings", "bone density ct"), "Observations:\nChanges in bone density and structure.\n\nPossible Findings:\nIndicates fractures, lesions, or degeneration.\n\nDifferential Diagnosis:\n- Fracture\n- Tumor\n- Infection\n\nRecommendation:\nCorrelate with clinical symptoms and history.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("multiphasic ct liver scan", "hcc screening"), "Observations:\nImaging done in arterial, portal venous, and delayed phases.\n\nPossible Findings:\nHelps detect liver lesions and vascular patterns.\n\nDifferential Diagnosis:\n- Hepatocellular carcinoma\n- Hemangioma\n- Metastasis\n\nRecommendation:\nUse contrast timing properly for accurate diagnosis.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("ct radiation dose alara", "patient radiation safety"), "Observations:\nRadiation exposure minimized while maintaining image quality.\n\nPossible Findings:\nEnsures patient safety during imaging.\n\nDifferential Considerations:\n- Dose optimization\n- Patient safety protocols\n\nRecommendation:\nFollow ALARA principle to reduce unnecessary exposure.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("prostate pi-rads criteria", "prostate mri staging"), "Observations:\nAssessment of prostate lesions using standardized scoring.\n\nPossible Findings:\nHelps identify risk of prostate cancer.\n\nDifferential Diagnosis:\n- Benign lesion\n- Suspicious tumor\n\nRecommendation:\nUse PI-RADS scoring for structured reporting.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("shoulder labrum tear mri", "slap tear mri"), "Observations:\nIrregular labrum contour with signal changes.\n\nPossible Findings:\nIndicates labral tear or injury.\n\nDifferential Diagnosis:\n- SLAP tear\n- Degenerative changes\n\nRecommendation:\nOrthopedic evaluation recommended.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("evaluate liver hemangioma mri", "lightbulb sign mri"), "Observations:\nWell-defined lesion with characteristic enhancement.\n\nPossible Findings:\nSuggests benign vascular tumor.\n\nDifferential Diagnosis:\n- Hemangioma\n- Metastasis\n\nRecommendation:\nFollow-up imaging may be required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("breast mri imaging protocols", "breast contrast mri"), "Observations:\nUse of contrast-enhanced dynamic sequences.\n\nPossible Findings:\nHelps detect lesions and vascular patterns.\n\nDifferential Diagnosis:\n- Benign lesion\n- Malignant tumor\n\nRecommendation:\nFollow standardized breast MRI protocol.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("basic mri sequence types", "brain mri sequences"), "Observations:\nDifferent sequences provide varied tissue contrast.\n\nPossible Findings:\nUsed for detailed tissue analysis.\n\nDifferential Considerations:\n- T1, T2, FLAIR, DWI, ADC, SWI\n\nRecommendation:\nUse multiple sequences for accurate diagnosis.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("myelitis signs mri", "spinal cord inflammation"), "Observations:\nSignal changes in spinal cord.\n\nPossible Findings:\nIndicates inflammation of spinal cord.\n\nDifferential Diagnosis:\n- Myelitis\n- Multiple sclerosis\n\nRecommendation:\nNeurological evaluation required.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician."),
            KBEntry(listOf("basics pelvis mri anatomy", "pelvis staging mri"), "Observations:\nVisualization of pelvic organs and structures.\n\nPossible Findings:\nHelps detect abnormalities in pelvic region.\n\nDifferential Diagnosis:\n- Tumor\n- Infection\n- Structural abnormality\n\nRecommendation:\nUse appropriate MRI sequences for evaluation.\n\nThis analysis is AI-assisted and should be reviewed by a qualified radiologist or physician.")
        )
    }
}
