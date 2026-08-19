/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.AppSettings
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.SettingsRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio.AudioFeedbackManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.capture.AutoCaptureEngine
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.capture.AutoCaptureState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionActivityType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionDetectionManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application,
    private val motionDetectionManager: MotionDetectionManager,
    private val autoCaptureEngine: AutoCaptureEngine,
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository,
    private val audioFeedbackManager: AudioFeedbackManager,
    val wearablesViewModel: WearablesViewModel
) : AndroidViewModel(application) {

    val motionStatus: StateFlow<MotionStatus> = motionDetectionManager.motionStatus
    val captureState: StateFlow<AutoCaptureState> = autoCaptureEngine.engineState
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    val latestReport: StateFlow<LocalReport?> = reportRepository.reportsFlow
        .map { list -> list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalReportsCount: StateFlow<Int> = reportRepository.reportsFlow
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun triggerManualCapture() {
        autoCaptureEngine.triggerManualCapture()
    }

    fun toggleAutoCapture() {
        autoCaptureEngine.toggleAutoCapture()
    }

    fun simulateMotion(activityType: MotionActivityType) {
        motionDetectionManager.simulateMotionState(activityType)
    }

    fun testAudioFeedback() {
        viewModelScope.launch {
            audioFeedbackManager.speakSuccess("Teste de áudio: Registro enviado com sucesso.", true)
        }
    }
}
