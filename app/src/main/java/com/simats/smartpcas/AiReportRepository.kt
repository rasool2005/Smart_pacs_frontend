package com.simats.smartpcas

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

class AiReportRepository(private val context: Context? = null) {
    
    private val gson = Gson()
    private val sharedPrefs = context?.getSharedPreferences("ai_reports_prefs", Context.MODE_PRIVATE)

    private fun getLocalReports(userId: Int): MutableList<AiReport> {
        val json = sharedPrefs?.getString("local_reports_$userId", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<AiReport>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveLocalReports(userId: Int, reports: List<AiReport>) {
        sharedPrefs?.edit()?.putString("local_reports_$userId", gson.toJson(reports))?.apply()
    }

    private fun getDeletedIds(userId: Int): MutableSet<Int> {
        val set = sharedPrefs?.getStringSet("deleted_report_ids_$userId", emptySet()) ?: emptySet()
        return set.map { it.toInt() }.toMutableSet()
    }

    private fun saveDeletedIds(userId: Int, ids: Set<Int>) {
        sharedPrefs?.edit()?.putStringSet("deleted_report_ids_$userId", ids.map { it.toString() }.toSet())?.apply()
    }

    suspend fun saveReport(request: SaveReportRequest): Resource<SimpleResponse> = withContext(Dispatchers.IO) {
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLocalReport = AiReport(
            id = System.currentTimeMillis().toInt(),
            user_id = request.user_id,
            examination_type = request.examination_type,
            confidence_score = request.confidence_score,
            confidence_level = request.confidence_level,
            finding_name = request.finding_name,
            location = request.location,
            observation = request.observation,
            severity = request.severity,
            impression = request.impression,
            created_at = currentTime,
            image_uri = request.image_uri
        )
        
        val localReports = getLocalReports(request.user_id)
        localReports.add(0, newLocalReport)
        saveLocalReports(request.user_id, localReports)

        try {
            val response = ApiClient.apiService.saveAiReport(request)
            if (response.isSuccessful && response.body() != null) {
                val serverResponse = response.body()!!
                if (serverResponse.report_id != null) {
                    // Update local version with the backend-assigned ID
                    val currentLocal = getLocalReports(request.user_id)
                    val idx = currentLocal.indexOfFirst { it.id == newLocalReport.id }
                    if (idx != -1) {
                        currentLocal[idx] = currentLocal[idx].copy(id = serverResponse.report_id)
                        saveLocalReports(request.user_id, currentLocal)
                    }
                }
                Resource.Success(serverResponse)
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Success(SimpleResponse("local_success", "Saved locally"))
        }
    }

    suspend fun getReports(userId: Int): Resource<List<AiReport>> = withContext(Dispatchers.IO) {
        val localReports = getLocalReports(userId)
        val deletedIds = getDeletedIds(userId)

        try {
            val response = ApiClient.apiService.getAiReports(userId)
            val backendReports = if (response.isSuccessful && response.body() != null) {
                val reports = response.body()!!.reports ?: emptyList()
                reports
            } else {
                emptyList()
            }

            // Sync: If a report exists in both, prefer the local one (which has the image_uri)
            val filteredLocalReports = localReports.filter { it.user_id == userId }
            val combined = (filteredLocalReports + backendReports)
                .distinctBy { it.id }
                .filter { !deletedIds.contains(it.id) }
                .map { report ->
                    // If backend report doesn't have image_uri, try to find it in localReports
                    if (report.image_uri.isNullOrEmpty()) {
                        localReports.find { it.id == report.id }?.image_uri?.let { 
                            report.copy(image_uri = it)
                        } ?: report
                    } else {
                        report
                    }
                }
                .sortedByDescending { it.created_at }
            
            Resource.Success(combined)
        } catch (e: Exception) {
            val filteredLocal = localReports.filter { it.user_id == userId && !deletedIds.contains(it.id) }
            Resource.Success(filteredLocal)
        }
    }

    suspend fun deleteReport(userId: Int, reportId: Int): Resource<SimpleResponse> = withContext(Dispatchers.IO) {
        val deletedIds = getDeletedIds(userId)
        deletedIds.add(reportId)
        saveDeletedIds(userId, deletedIds)

        val localReports = getLocalReports(userId)
        localReports.removeAll { it.id == reportId }
        saveLocalReports(userId, localReports)

        try {
            ApiClient.apiService.deleteReport(reportId)
        } catch (e: Exception) {}

        Resource.Success(SimpleResponse("success", "Deleted"))
    }

    suspend fun sendEmail(request: SendEmailRequest): Resource<SimpleResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.sendReportEmail(request)
            safeApiCall(response)
        } catch (e: Exception) { Resource.Error("Error") }
    }

    suspend fun downloadReport(reportId: Int): Resource<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.downloadReport(reportId)
            safeApiCall(response)
        } catch (e: Exception) { Resource.Error("Error") }
    }

    private fun <T> safeApiCall(response: Response<T>): Resource<T> {
        return if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error("Error ${response.code()}")
        }
    }
}
