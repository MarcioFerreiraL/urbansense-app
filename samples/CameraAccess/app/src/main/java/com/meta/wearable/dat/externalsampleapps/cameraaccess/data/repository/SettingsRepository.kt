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
    val apiUrl: String = "https://urbansense-ai.marciodev.com",
    val authToken: String = "",
    val autoCaptureIntervalSec: Int = 15,
    val isMotionDetectionEnabled: Boolean = true,
    val isAudioVoiceFeedbackEnabled: Boolean = true,
    val deviceId: String = "rayban_meta_01",
    // Notificação por e-mail (Mailtrap) — como cada ocorrência chega à prefeitura hoje, sem
    // depender de um backend próprio.
    // Sandbox por padrão: os e-mails caem na caixa de testes do Mailtrap em vez de saírem de
    // verdade — seguro para demonstração. Troque o {inbox_id} pelo da sua conta em Ajustes, ou
    // aponte para "https://send.api.mailtrap.io/api/send" quando o domínio de produção estiver
    // verificado.
    val mailtrapApiUrl: String = "https://sandbox.api.mailtrap.io/api/send/{inbox_id}",
    val mailtrapApiToken: String = "",
    val mailtrapSenderEmail: String = "relatos@urbansense.ai",
    val cityHallEmail: String = "ouvidoria@surubim.pe.gov.br"
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

        val rawUrl = prefs.getString(KEY_API_URL, null)
        val validUrl = if (rawUrl.isNullOrBlank() || rawUrl.contains("/v1") || rawUrl.contains("/reports") || !rawUrl.contains("predict")) {
            "https://urbansense-ai.marciodev.com/predict"
        } else {
            rawUrl
        }
        if (rawUrl != validUrl) {
            prefs.edit().putString(KEY_API_URL, validUrl).apply()
        }

        return AppSettings(
            apiUrl = validUrl,
            authToken = prefs.getString(KEY_AUTH_TOKEN, "") ?: "",
            autoCaptureIntervalSec = prefs.getInt(KEY_AUTO_INTERVAL, 15),
            isMotionDetectionEnabled = prefs.getBoolean(KEY_MOTION_ENABLED, true),
            isAudioVoiceFeedbackEnabled = prefs.getBoolean(KEY_AUDIO_FEEDBACK, true),
            deviceId = devId,
            mailtrapApiUrl = prefs.getString(KEY_MAILTRAP_URL, "https://sandbox.api.mailtrap.io/api/send/{inbox_id}") ?: "https://sandbox.api.mailtrap.io/api/send/{inbox_id}",
            mailtrapApiToken = prefs.getString(KEY_MAILTRAP_TOKEN, "") ?: "",
            mailtrapSenderEmail = prefs.getString(KEY_MAILTRAP_SENDER, "relatos@urbansense.ai") ?: "relatos@urbansense.ai",
            cityHallEmail = prefs.getString(KEY_CITY_HALL_EMAIL, "ouvidoria@surubim.pe.gov.br") ?: "ouvidoria@surubim.pe.gov.br"
        )
    }

    fun updateSettings(
        apiUrl: String? = null,
        authToken: String? = null,
        autoCaptureIntervalSec: Int? = null,
        isMotionDetectionEnabled: Boolean? = null,
        isAudioVoiceFeedbackEnabled: Boolean? = null,
        deviceId: String? = null,
        mailtrapApiUrl: String? = null,
        mailtrapApiToken: String? = null,
        mailtrapSenderEmail: String? = null,
        cityHallEmail: String? = null
    ) {
        val current = _settingsFlow.value
        val updated = current.copy(
            apiUrl = apiUrl ?: current.apiUrl,
            authToken = authToken ?: current.authToken,
            autoCaptureIntervalSec = autoCaptureIntervalSec ?: current.autoCaptureIntervalSec,
            isMotionDetectionEnabled = isMotionDetectionEnabled ?: current.isMotionDetectionEnabled,
            isAudioVoiceFeedbackEnabled = isAudioVoiceFeedbackEnabled ?: current.isAudioVoiceFeedbackEnabled,
            deviceId = deviceId ?: current.deviceId,
            mailtrapApiUrl = mailtrapApiUrl ?: current.mailtrapApiUrl,
            mailtrapApiToken = mailtrapApiToken ?: current.mailtrapApiToken,
            mailtrapSenderEmail = mailtrapSenderEmail ?: current.mailtrapSenderEmail,
            cityHallEmail = cityHallEmail ?: current.cityHallEmail
        )

        prefs.edit().apply {
            putString(KEY_API_URL, updated.apiUrl)
            putString(KEY_AUTH_TOKEN, updated.authToken)
            putInt(KEY_AUTO_INTERVAL, updated.autoCaptureIntervalSec)
            putBoolean(KEY_MOTION_ENABLED, updated.isMotionDetectionEnabled)
            putBoolean(KEY_AUDIO_FEEDBACK, updated.isAudioVoiceFeedbackEnabled)
            putString(KEY_DEVICE_ID, updated.deviceId)
            putString(KEY_MAILTRAP_URL, updated.mailtrapApiUrl)
            putString(KEY_MAILTRAP_TOKEN, updated.mailtrapApiToken)
            putString(KEY_MAILTRAP_SENDER, updated.mailtrapSenderEmail)
            putString(KEY_CITY_HALL_EMAIL, updated.cityHallEmail)
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
        private const val KEY_MAILTRAP_URL = "mailtrap_api_url"
        private const val KEY_MAILTRAP_TOKEN = "mailtrap_api_token"
        private const val KEY_MAILTRAP_SENDER = "mailtrap_sender_email"
        private const val KEY_CITY_HALL_EMAIL = "city_hall_email"
    }
}
