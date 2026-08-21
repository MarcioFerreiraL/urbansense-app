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
    // Quem recebe a notificação de cada ocorrência. O envio em si (detecção + e-mail via
    // Mailtrap) roda inteiramente em urbansense-api — o app só manda a foto/GPS pra `POST
    // /report` e diz pra quem mandar; o token do Mailtrap nunca fica no cliente.
    // Placeholder fictício de demo: o projeto Mailtrap está em modo Email Testing (sandbox),
    // então nada é entregue de verdade pra esse endereço — a mensagem só aparece no inbox de
    // teste do painel do Mailtrap. Evita confusão com o e-mail real da ouvidoria de Surubim.
    val cityHallEmail: String = "ouvidoria@urbansense-demo.com"
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

        var email = prefs.getString(KEY_CITY_HALL_EMAIL, "ouvidoria@urbansense-demo.com") ?: "ouvidoria@urbansense-demo.com"
        if (email.isEmpty() ||
            email == "ouvidoria@surubim.pe.gov.br" ||
            email == "marcio.flima@upe.br" ||
            email == "gabriel.lopes.albuquerque@gmail.com"
        ) {
            email = "ouvidoria@urbansense-demo.com"
            prefs.edit().putString(KEY_CITY_HALL_EMAIL, email).apply()
        }

        return AppSettings(
            apiUrl = prefs.getString(KEY_API_URL, "https://urbansense-ai.marciodev.com") ?: "https://urbansense-ai.marciodev.com",
            authToken = prefs.getString(KEY_AUTH_TOKEN, "") ?: "",
            autoCaptureIntervalSec = prefs.getInt(KEY_AUTO_INTERVAL, 15),
            isMotionDetectionEnabled = prefs.getBoolean(KEY_MOTION_ENABLED, true),
            isAudioVoiceFeedbackEnabled = prefs.getBoolean(KEY_AUDIO_FEEDBACK, true),
            deviceId = devId,
            cityHallEmail = email
        )
    }

    fun updateSettings(
        apiUrl: String? = null,
        authToken: String? = null,
        autoCaptureIntervalSec: Int? = null,
        isMotionDetectionEnabled: Boolean? = null,
        isAudioVoiceFeedbackEnabled: Boolean? = null,
        deviceId: String? = null,
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
            cityHallEmail = cityHallEmail ?: current.cityHallEmail
        )

        prefs.edit().apply {
            putString(KEY_API_URL, updated.apiUrl)
            putString(KEY_AUTH_TOKEN, updated.authToken)
            putInt(KEY_AUTO_INTERVAL, updated.autoCaptureIntervalSec)
            putBoolean(KEY_MOTION_ENABLED, updated.isMotionDetectionEnabled)
            putBoolean(KEY_AUDIO_FEEDBACK, updated.isAudioVoiceFeedbackEnabled)
            putString(KEY_DEVICE_ID, updated.deviceId)
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
        private const val KEY_CITY_HALL_EMAIL = "city_hall_email"
    }
}
