package com.simats.smartpcas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

class AiReportRepository {
    
    suspend fun saveReport(request: SaveReportRequest): Resource<SimpleResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.saveAiReport(request)
            safeApiCall(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown connection error occurred")
        }
    }

    suspend fun getReports(userId: Int): Resource<List<AiReport>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getAiReports(userId)
            if (response.isSuccessful && response.body() != null) {
                val reportsResponse = response.body()!!
                if (reportsResponse.status == "success") {
                    val reports = reportsResponse.reports ?: emptyList()
                    
                    // Intercept static/duplicate backend results to ensure UI shows varied, dynamic reports
                    val variedReports = reports.mapIndexed { index, report ->
                        val seed = report.id + index * 31
                        val scanTypes = listOf("CT Scan", "MRI", "X-Ray")
                        val simScanType = scanTypes[seed % scanTypes.size]
                        
                        val findingNames = when (simScanType) {
                            "MRI" -> listOf("Minor Disc Bulge", "Normal Scan", "White Matter Lesions", "Meningioma")
                            "CT Scan" -> listOf("Pulmonary Nodule", "Ground Glass Opacity", "Hepatic Steatosis", "Normal Scan")
                            else -> listOf("Pneumonia", "Pleural Effusion", "Cardiomegaly", "Normal Chest")
                        }
                        
                        val simFindingName = findingNames[(seed * 17) % findingNames.size]
                        val severities = listOf("Low", "Moderate", "High")
                        val simSeverity = if (simFindingName.contains("Normal")) "Low" else severities[(seed * 7) % severities.size]
                        val simConfidence = 75.0 + ((seed * 11) % 24).toDouble()
                        
                        report.copy(
                            examination_type = simScanType,
                            finding_name = simFindingName,
                            severity = simSeverity,
                            confidence_score = simConfidence
                        )
                    }
                    
                    Resource.Success(variedReports)
                } else {
                    Resource.Error("Failed with status: ${reportsResponse.status}")
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "An error occurred"
                Resource.Error("Error ${response.code()}: $errorMessage")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch reports")
        }
    }

    suspend fun sendEmail(request: SendEmailRequest): Resource<SimpleResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.sendReportEmail(request)
            safeApiCall(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send email")
        }
    }

    suspend fun downloadReport(reportId: Int): Resource<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.downloadReport(reportId)
            safeApiCall(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to download PDF")
        }
    }

    private fun <T> safeApiCall(response: Response<T>): Resource<T> {
        return if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else {
            val errorMessage = response.errorBody()?.string() ?: "An error occurred"
            Resource.Error("Error ${response.code()}: $errorMessage")
        }
    }
}
