/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.MailtrapEmailDispatcher
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.NativeHttpDispatcher
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportItem
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.TriggerType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReportStore
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.email.ReportEmailTemplate
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio.AudioFeedbackManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.location.LocationManagerHelper
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.location.TaggedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ReportSubmissionResult {
    data class Success(val reportId: String, val audioFeedback: String) : ReportSubmissionResult()
    data class Failure(val errorMsg: String, val audioFeedback: String, val isOfflineSaved: Boolean) : ReportSubmissionResult()
}

class ReportRepository(
    private val context: Context,
    private val reportStore: LocalReportStore,
    private val settingsRepository: SettingsRepository,
    private val locationManagerHelper: LocationManagerHelper,
    private val audioFeedbackManager: AudioFeedbackManager
) {

    companion object {
        private const val TAG = "UrbanSense:Repo"
    }

    val reportsFlow: Flow<List<LocalReport>> = reportStore.reportsFlow

    suspend fun submitReport(
        bitmap: Bitmap,
        triggerType: TriggerType
    ): ReportSubmissionResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val tempId = "rep_" + UUID.randomUUID().toString()

        // 1. Tag Location on-demand (GPS at the millisecond of capture)
        val location: TaggedLocation = locationManagerHelper.getOnDemandLocation()

        // 2. Save compressed JPEG to cache dir
        val imageFile = saveBitmapToCache(bitmap, tempId)

        // 3. Prepare initial local record
        var localRecord = LocalReport(
            id = tempId,
            localImagePath = imageFile?.absolutePath,
            remoteThumbnailUrl = null,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.timestampIso,
            triggerType = triggerType.name,
            status = ReportStatus.PENDING.name,
            createdAtMillis = System.currentTimeMillis()
        )
        reportStore.insertOrUpdate(localRecord)

        if (imageFile == null) {
            val feedback = "Falha ao processar a imagem capturada."
            localRecord = localRecord.copy(
                status = ReportStatus.FAILED.name,
                errorMessage = "Falha ao salvar imagem local",
                audioFeedback = feedback
            )
            reportStore.insertOrUpdate(localRecord)
            audioFeedbackManager.speakFailure(feedback, settings.isAudioVoiceFeedbackEnabled)
            return@withContext ReportSubmissionResult.Failure("Erro ao salvar imagem", feedback, false)
        }

        // 4. Notify the city hall by e-mail (Mailtrap) — the citizen-facing delivery channel.
        try {
            val emailData = ReportEmailTemplate.ReportEmailData(
                reportId = tempId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestampIso = location.timestampIso,
                triggerLabel = triggerLabelPlain(triggerType),
                deviceId = settings.deviceId,
                cityHallName = "Prefeitura Municipal de Surubim",
            )

            val result = MailtrapEmailDispatcher.sendReportEmail(
                apiUrl = settings.mailtrapApiUrl,
                apiToken = settings.mailtrapApiToken,
                fromEmail = settings.mailtrapSenderEmail,
                toEmail = settings.cityHallEmail,
                subject = ReportEmailTemplate.subject(emailData),
                htmlBody = ReportEmailTemplate.buildHtml(emailData),
                imageFile = imageFile,
            )

            if (result.success) {
                val audioFeedback = "Registro enviado por e-mail para a Prefeitura de Surubim."

                localRecord = localRecord.copy(
                    status = ReportStatus.QUEUED.name,
                    audioFeedback = audioFeedback,
                    errorMessage = null,
                )
                reportStore.insertOrUpdate(localRecord)

                audioFeedbackManager.speakSuccess(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Success(tempId, audioFeedback)
            } else {
                val audioFeedback = "Falha ao enviar e-mail. Registro salvo localmente."

                localRecord = localRecord.copy(
                    status = ReportStatus.LOCAL_SAVED.name,
                    audioFeedback = audioFeedback,
                    errorMessage = result.message ?: "Falha no envio do e-mail (HTTP ${result.statusCode})",
                )
                reportStore.insertOrUpdate(localRecord)

                audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Failure(
                    errorMsg = result.message ?: "HTTP ${result.statusCode}",
                    audioFeedback = audioFeedback,
                    isOfflineSaved = true,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending report e-mail", e)
            val audioFeedback = "Sem conexão. Registro salvo como pendente."

            localRecord = localRecord.copy(
                status = ReportStatus.PENDING.name,
                audioFeedback = audioFeedback,
                errorMessage = e.localizedMessage ?: "Falha na conexão com o Mailtrap"
            )
            reportStore.insertOrUpdate(localRecord)

            audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
            ReportSubmissionResult.Failure(
                errorMsg = e.localizedMessage ?: "Erro de rede",
                audioFeedback = audioFeedback,
                isOfflineSaved = true
            )
        }
    }

    private fun triggerLabelPlain(triggerType: TriggerType): String =
        if (triggerType == TriggerType.AUTOMATIC) "Captura automática" else "Captura manual"

    suspend fun retryReportSubmission(reportId: String): ReportSubmissionResult = withContext(Dispatchers.IO) {
        val localReport = reportStore.getReportById(reportId)
            ?: return@withContext ReportSubmissionResult.Failure("Registro não encontrado", "Registro não encontrado", false)

        val imagePath = localReport.localImagePath
        if (imagePath.isNullOrEmpty()) {
            return@withContext ReportSubmissionResult.Failure("Arquivo da imagem não encontrado", "Imagem não encontrada localmente", false)
        }

        val imageFile = java.io.File(imagePath)
        if (!imageFile.exists()) {
            return@withContext ReportSubmissionResult.Failure("Arquivo da imagem não existe", "Arquivo da imagem não existe no dispositivo", false)
        }

        val settings = settingsRepository.getSettings()

        try {
            val emailData = ReportEmailTemplate.ReportEmailData(
                reportId = localReport.id,
                latitude = localReport.latitude,
                longitude = localReport.longitude,
                accuracy = localReport.accuracy,
                timestampIso = localReport.timestamp,
                triggerLabel = triggerLabelPlain(
                    runCatching { TriggerType.valueOf(localReport.triggerType.orEmpty()) }
                        .getOrDefault(TriggerType.MANUAL)
                ),
                deviceId = settings.deviceId,
                cityHallName = "Prefeitura Municipal de Surubim",
            )

            val result = MailtrapEmailDispatcher.sendReportEmail(
                apiUrl = settings.mailtrapApiUrl,
                apiToken = settings.mailtrapApiToken,
                fromEmail = settings.mailtrapSenderEmail,
                toEmail = settings.cityHallEmail,
                subject = ReportEmailTemplate.subject(emailData),
                htmlBody = ReportEmailTemplate.buildHtml(emailData),
                imageFile = imageFile,
            )

            if (result.success) {
                val audioFeedback = "Registro reenviado por e-mail com sucesso."
                val updatedRecord = localReport.copy(
                    status = ReportStatus.QUEUED.name,
                    audioFeedback = audioFeedback,
                    errorMessage = null
                )
                reportStore.insertOrUpdate(updatedRecord)
                audioFeedbackManager.speakSuccess(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Success(localReport.id, audioFeedback)
            } else {
                val audioFeedback = "Falha no reenvio do e-mail. Registro mantido como pendente."
                val updatedRecord = localReport.copy(
                    status = ReportStatus.PENDING.name,
                    audioFeedback = audioFeedback,
                    errorMessage = result.message ?: "HTTP ${result.statusCode}"
                )
                reportStore.insertOrUpdate(updatedRecord)
                audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Failure(result.message ?: "HTTP ${result.statusCode}", audioFeedback, true)
            }
        } catch (e: Exception) {
            val audioFeedback = "Servidor indisponível. Mantido como pendente."
            val updatedRecord = localReport.copy(
                status = ReportStatus.PENDING.name,
                audioFeedback = audioFeedback,
                errorMessage = e.localizedMessage ?: "Erro de rede"
            )
            reportStore.insertOrUpdate(updatedRecord)
            audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
            ReportSubmissionResult.Failure(e.localizedMessage ?: "Erro de rede", audioFeedback, true)
        }
    }

    suspend fun fetchRemoteHistory(): Result<List<ReportItem>> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        try {
            val (statusCode, response) = NativeHttpDispatcher.fetchReportsHistory(
                baseUrl = settings.apiUrl,
                authToken = settings.authToken,
                page = 1,
                limit = 50
            )

            if (statusCode in 200..299) {
                val items = response.data
                items.forEach { item ->
                    reportStore.insertOrUpdate(
                        LocalReport(
                            id = item.id,
                            remoteThumbnailUrl = item.thumbnailUrl,
                            latitude = item.latitude,
                            longitude = item.longitude,
                            accuracy = item.accuracy,
                            timestamp = item.createdAt,
                            triggerType = item.triggerType,
                            status = item.status,
                            detectionResult = item.detectionResult,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                Result.success(items)
            } else {
                Result.failure(Exception("HTTP $statusCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote history", e)
            Result.failure(e)
        }
    }

    suspend fun clearHistory() {
        reportStore.clearAll()
    }

    private fun saveBitmapToCache(bitmap: Bitmap, reportId: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val file = File(cacheDir, "$reportId.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            null
        }
    }
}
