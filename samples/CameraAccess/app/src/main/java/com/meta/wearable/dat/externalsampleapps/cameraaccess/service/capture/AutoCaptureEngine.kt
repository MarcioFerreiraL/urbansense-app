/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.service.capture

import android.graphics.Bitmap
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.TriggerType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportSubmissionResult
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.SettingsRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionActivityType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionDetectionManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AutoCaptureState(
    val isAutoCaptureActive: Boolean = true,
    val isSleepMode: Boolean = true,
    val isCapturingNow: Boolean = false,
    val lastSubmissionResult: ReportSubmissionResult? = null,
    val totalCapturesInSession: Int = 0,
    val countdownSec: Int = 0,
    val lastTriggerType: TriggerType? = null
)

class AutoCaptureEngine(
    private val motionDetectionManager: MotionDetectionManager,
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository,
    private val capturePhotoProvider: suspend () -> Bitmap?
) {

    companion object {
        private const val TAG = "UrbanSense:CaptureEngine"
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _engineState = MutableStateFlow(AutoCaptureState())
    val engineState: StateFlow<AutoCaptureState> = _engineState.asStateFlow()

    private var autoCaptureLoopJob: Job? = null
    private var isStreamingActive = false

    init {
        // Monitor motion transitions
        engineScope.launch {
            motionDetectionManager.motionStatus.collect { motion ->
                onMotionChanged(motion)
            }
        }
    }

    fun setStreamingActive(active: Boolean) {
        isStreamingActive = active
        evaluateEngineState()
    }

    fun toggleAutoCapture() {
        _engineState.update { it.copy(isAutoCaptureActive = !it.isAutoCaptureActive) }
        evaluateEngineState()
    }

    private fun onMotionChanged(motion: MotionStatus) {
        val settings = settingsRepository.getSettings()
        val shouldSleep = if (settings.isMotionDetectionEnabled) !motion.isMoving else false

        _engineState.update { it.copy(isSleepMode = shouldSleep) }
        evaluateEngineState()
    }

    private fun evaluateEngineState() {
        val state = _engineState.value
        if (state.isAutoCaptureActive && !state.isSleepMode && isStreamingActive) {
            startAutoCaptureLoop()
        } else {
            stopAutoCaptureLoop()
        }
    }

    private fun startAutoCaptureLoop() {
        if (autoCaptureLoopJob?.isActive == true) return

        autoCaptureLoopJob = engineScope.launch {
            Log.i(TAG, "Starting Auto-Capture Periodic Loop")
            while (isActive) {
                val interval = settingsRepository.getSettings().autoCaptureIntervalSec.coerceAtLeast(5)

                // Countdown timer for UI display
                for (remaining in interval downTo 1) {
                    _engineState.update { it.copy(countdownSec = remaining) }
                    delay(1000L)
                    if (!isActive || _engineState.value.isSleepMode || !_engineState.value.isAutoCaptureActive) {
                        return@launch
                    }
                }
                _engineState.update { it.copy(countdownSec = 0) }

                // Execute Auto Trigger
                performCapture(TriggerType.AUTOMATIC)
            }
        }
    }

    private fun stopAutoCaptureLoop() {
        autoCaptureLoopJob?.cancel()
        autoCaptureLoopJob = null
        _engineState.update { it.copy(countdownSec = 0) }
    }

    fun triggerManualCapture() {
        engineScope.launch {
            performCapture(TriggerType.MANUAL)
        }
    }

    private suspend fun performCapture(triggerType: TriggerType) {
        if (_engineState.value.isCapturingNow) return
        _engineState.update { it.copy(isCapturingNow = true, lastTriggerType = triggerType) }

        try {
            Log.d(TAG, "Triggering capture photo with triggerType: $triggerType")
            val bitmap = capturePhotoProvider()

            if (bitmap != null) {
                val result = reportRepository.submitReport(bitmap, triggerType)
                _engineState.update {
                    it.copy(
                        isCapturingNow = false,
                        lastSubmissionResult = result,
                        totalCapturesInSession = it.totalCapturesInSession + 1
                    )
                }
            } else {
                Log.w(TAG, "Capture provider returned null bitmap")
                _engineState.update { it.copy(isCapturingNow = false) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing capture", e)
            _engineState.update { it.copy(isCapturingNow = false) }
        }
    }
}
