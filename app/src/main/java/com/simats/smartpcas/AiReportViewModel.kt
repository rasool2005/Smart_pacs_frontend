package com.simats.smartpcas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class AiReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AiReportRepository(application)

    private val _saveReportState = MutableStateFlow<Resource<SimpleResponse>?>(null)
    val saveReportState: StateFlow<Resource<SimpleResponse>?> = _saveReportState.asStateFlow()

    private val _deleteReportState = MutableStateFlow<Resource<SimpleResponse>?>(null)
    val deleteReportState: StateFlow<Resource<SimpleResponse>?> = _deleteReportState.asStateFlow()

    // State for fetching reports list
    private val _reportsListState = MutableStateFlow<Resource<List<AiReport>>>(Resource.Loading())
    val reportsListState: StateFlow<Resource<List<AiReport>>> = _reportsListState.asStateFlow()

    // State for sending report email
    private val _sendEmailState = MutableStateFlow<Resource<SimpleResponse>?>(null)
    val sendEmailState: StateFlow<Resource<SimpleResponse>?> = _sendEmailState.asStateFlow()

    // State for downloading report PDF
    private val _downloadState = MutableStateFlow<Resource<ResponseBody>?>(null)
    val downloadState: StateFlow<Resource<ResponseBody>?> = _downloadState.asStateFlow()

    fun saveAiReport(request: SaveReportRequest) {
        viewModelScope.launch {
            _saveReportState.value = Resource.Loading()
            val result = repository.saveReport(request)
            _saveReportState.value = result
        }
    }

    fun deleteReport(userId: Int, reportId: Int) {
        viewModelScope.launch {
            _deleteReportState.value = Resource.Loading()
            val result = repository.deleteReport(userId, reportId)
            _deleteReportState.value = result
        }
    }

    fun getAiReports(userId: Int) {
        viewModelScope.launch {
            // 1. Emit loading state
            _reportsListState.value = Resource.Loading()
            
            // 2. Immediately emit local reports first for instant UI response
            val localReports = repository.getLocalReports(userId)
            val deletedIds = repository.getDeletedIds(userId)
            val initialList = localReports
                .filter { it.user_id == userId && !deletedIds.contains(it.id) }
                .sortedByDescending { it.created_at }
            
            if (initialList.isNotEmpty()) {
                _reportsListState.value = Resource.Success(initialList)
            }
            
            // 3. Then fetch refreshed data from combined sources
            val result = repository.getReports(userId)
            _reportsListState.value = result
        }
    }

    fun sendReportEmail(request: SendEmailRequest) {
        viewModelScope.launch {
            _sendEmailState.value = Resource.Loading()
            val result = repository.sendEmail(request)
            _sendEmailState.value = result
        }
    }

    fun downloadReportPdf(reportId: Int) {
        viewModelScope.launch {
            _downloadState.value = Resource.Loading()
            val result = repository.downloadReport(reportId)
            _downloadState.value = result
        }
    }

    // Call this to reset the single-event states after observing (like toasts or downloads)
    fun resetSaveState() { _saveReportState.value = null }
    fun resetEmailState() { _sendEmailState.value = null }
    fun resetDownloadState() { _downloadState.value = null }
}
