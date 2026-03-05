package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_report)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back Navigation
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val etAiReport = findViewById<EditText>(R.id.etAiReport)

        // Retrieve the dynamic prediction data from Intent
        val prediction = intent.getSerializableExtra("prediction_results") as? PredictionResponse

        if (prediction != null) {
            generateDynamicReport(etAiReport, prediction)
        }

        // Save Report
        findViewById<Button>(R.id.btnSaveReport).setOnClickListener {
            Toast.makeText(this, "Report Saved Successfully", Toast.LENGTH_SHORT).show()
        }


        // Send (Share Intent)
        findViewById<android.view.View>(R.id.btnSend).setOnClickListener {
            val reportText = etAiReport.text.toString()
            if (reportText.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Medical Report - Smart PCAS")
                    putExtra(Intent.EXTRA_TEXT, reportText)
                }
                startActivity(Intent.createChooser(shareIntent, "Send Report via"))
            } else {
                Toast.makeText(this, "Report is empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Copy AI Report
        findViewById<ImageView>(R.id.btnCopy).setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("AI Report", etAiReport.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateDynamicReport(editText: EditText, prediction: PredictionResponse) {
        val reportBuilder = StringBuilder()
        
        reportBuilder.append("EXAMINATION: ${prediction.scan_type} SCAN\n\n")
        
        reportBuilder.append("AI ANALYSIS METRICS:\n")
        reportBuilder.append("- Confidence Score: ${prediction.confidence_score}%\n")
        reportBuilder.append("- Confidence Level: ${prediction.confidence_level}\n\n")
        
        reportBuilder.append("AI FINDINGS:\n")
        if (prediction.findings != null && prediction.findings.isNotEmpty()) {
            prediction.findings.forEachIndexed { index, finding ->
                reportBuilder.append("${index + 1}. ${finding.title}\n")
                reportBuilder.append("   - Location: ${finding.location}\n")
                reportBuilder.append("   - Observation: ${finding.description}\n")
                reportBuilder.append("   - Severity: ${finding.severity}\n\n")
            }
        } else {
            reportBuilder.append("- No acute abnormalities detected by the model.\n")
            reportBuilder.append("- Message: ${prediction.message}\n\n")
        }
        
        reportBuilder.append("AI IMPRESSION:\n")
        if (prediction.findings != null && prediction.findings.isNotEmpty()) {
            prediction.findings.forEach { finding ->
                reportBuilder.append("- Evidence of ${finding.title} localized to ${finding.location}.\n")
            }
            reportBuilder.append("- Radiologist correlation is strongly advised.\n")
        } else {
            reportBuilder.append("- Unremarkable ${prediction.scan_type} scan based on current AI classification.\n")
        }

        editText.setText(reportBuilder.toString())
    }
}
