/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AppSettings(
    val apiUrl: String = "https://api.urbanscience.ai/v1",
    val authToken: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.urbansense_demo_token",
    val autoCaptureIntervalSec: Int = 15,
    val isMotionDetectionEnabled: Boolean = true,
    val isAudioVoiceFeedbackEnabled: Boolean = true,
    val deviceId: String = "rayban_meta_01"
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        var devId = prefs.getString(KEY_DEVICE_ID, null)
        if (devId.isNullOrEmpty()) {
            devId = "rayban_" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }

        return AppSettings(
            apiUrl = prefs.getString(KEY_API_URL, "https://api.urbanscience.ai/v1") ?: "https://api.urbanscience.ai/v1",
            authToken = prefs.getString(KEY_AUTH_TOKEN, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.urbansense_demo_token") ?: "",
            autoCaptureIntervalSec = prefs.getInt(KEY_AUTO_INTERVAL, 15),
            isMotionDetectionEnabled = prefs.getBoolean(KEY_MOTION_ENABLED, true),
            isAudioVoiceFeedbackEnabled = prefs.getBoolean(KEY_AUDIO_FEEDBACK, true),
            deviceId = devId
        )
    }

    fun updateSettings(
        apiUrl: String? = null,
        authToken: String? = null,
        autoCaptureIntervalSec: Int? = null,
        isMotionDetectionEnabled: Boolean? = null,
        isAudioVoiceFeedbackEnabled: Boolean? = null,
        deviceId: String? = null
    ) {
        val current = _settingsFlow.value
        val updated = current.copy(
            apiUrl = apiUrl ?: current.apiUrl,
            authToken = authToken ?: current.authToken,
            autoCaptureIntervalSec = autoCaptureIntervalSec ?: current.autoCaptureIntervalSec,
            isMotionDetectionEnabled = isMotionDetectionEnabled ?: current.isMotionDetectionEnabled,
            isAudioVoiceFeedbackEnabled = isAudioVoiceFeedbackEnabled ?: current.isAudioVoiceFeedbackEnabled,
            deviceId = deviceId ?: current.deviceId
        )

        prefs.edit().apply {
            putString(KEY_API_URL, updated.apiUrl)
            putString(KEY_AUTH_TOKEN, updated.authToken)
            putInt(KEY_AUTO_INTERVAL, updated.autoCaptureIntervalSec)
            putBoolean(KEY_MOTION_ENABLED, updated.isMotionDetectionEnabled)
            putBoolean(KEY_AUDIO_FEEDBACK, updated.isAudioVoiceFeedbackEnabled)
            putString(KEY_DEVICE_ID, updated.deviceId)
        }.apply()

        _settingsFlow.value = updated
    }

    fun getSettings(): AppSettings = _settingsFlow.value

    companion object {
        private const val PREFS_NAME = "urbansense_settings"
        private const val KEY_API_URL = "api_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_AUTO_INTERVAL = "auto_interval"
        private const val KEY_MOTION_ENABLED = "motion_enabled"
        private const val KEY_AUDIO_FEEDBACK = "audio_feedback"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
