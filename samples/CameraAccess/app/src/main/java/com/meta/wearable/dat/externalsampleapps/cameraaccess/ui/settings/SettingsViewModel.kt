/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.AppSettings
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.SettingsRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio.AudioFeedbackManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val audioFeedbackManager: AudioFeedbackManager
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    fun updateApiUrl(url: String) {
        settingsRepository.updateSettings(apiUrl = url.trim())
    }

    fun updateAuthToken(token: String) {
        settingsRepository.updateSettings(authToken = token.trim())
    }

    fun updateAutoInterval(seconds: Int) {
        settingsRepository.updateSettings(autoCaptureIntervalSec = seconds)
    }

    fun toggleMotionDetection(enabled: Boolean) {
        settingsRepository.updateSettings(isMotionDetectionEnabled = enabled)
    }

    fun toggleAudioVoiceFeedback(enabled: Boolean) {
        settingsRepository.updateSettings(isAudioVoiceFeedbackEnabled = enabled)
    }

    fun updateMailtrapApiUrl(url: String) {
        settingsRepository.updateSettings(mailtrapApiUrl = url.trim())
    }

    fun updateMailtrapApiToken(token: String) {
        settingsRepository.updateSettings(mailtrapApiToken = token.trim())
    }

    fun updateMailtrapSenderEmail(email: String) {
        settingsRepository.updateSettings(mailtrapSenderEmail = email.trim())
    }

    fun updateCityHallEmail(email: String) {
        settingsRepository.updateSettings(cityHallEmail = email.trim())
    }

    fun testAudioFeedback() {
        viewModelScope.launch {
            audioFeedbackManager.speakSuccess(
                "Feedback de voz do UrbanSense AI ativado e funcionando nos óculos Meta Ray-Ban.",
                true
            )
        }
    }
}
