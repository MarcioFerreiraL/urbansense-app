/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isSyncing: Boolean = false,
    val syncErrorMessage: String? = null,
    val selectedReport: LocalReport? = null
)

class HistoryViewModel(
    application: Application,
    private val reportRepository: ReportRepository
) : AndroidViewModel(application) {

    val reports: StateFlow<List<LocalReport>> = reportRepository.reportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun refreshFromApi() {
        if (_uiState.value.isSyncing) return
        _uiState.value = _uiState.value.copy(isSyncing = true, syncErrorMessage = null)

        viewModelScope.launch {
            val result = reportRepository.fetchRemoteHistory()
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSyncing = false, syncErrorMessage = null)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncErrorMessage = error.localizedMessage ?: "Falha ao sincronizar com servidor"
                )
            }
        }
    }

    fun selectReport(report: LocalReport?) {
        _uiState.value = _uiState.value.copy(selectedReport = report)
    }

    fun retryReport(reportId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncErrorMessage = null)
            val result = reportRepository.retryReportSubmission(reportId)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncErrorMessage = if (result is com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportSubmissionResult.Failure) result.errorMsg else null
            )
            // Update selected report if open in dialog
            val updated = reports.value.find { it.id == reportId }
            if (updated != null) {
                _uiState.value = _uiState.value.copy(selectedReport = updated)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            reportRepository.clearHistory()
        }
    }
}
