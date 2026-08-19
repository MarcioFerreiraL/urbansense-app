/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.NativeHttpDispatcher
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportItem
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.TriggerType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReportStore
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

        // 4. Dispatch Multipart Request to API via NativeHttpDispatcher
        try {
            val (statusCode, response) = NativeHttpDispatcher.submitMultipartReport(
                baseUrl = settings.apiUrl,
                authToken = settings.authToken,
                imageFile = imageFile,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestampIso = location.timestampIso,
                triggerType = triggerType.name,
                deviceId = settings.deviceId
            )

            if (statusCode in 200..299) {
                val reportId = response.data?.reportId ?: tempId
                val audioFeedback = response.audioFeedback ?: "Registro enviado com sucesso."
                val remoteUrl = response.data?.thumbnailUrl

                // Free phone storage by deleting local temporary file after server upload
                try {
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete temporary local report file", e)
                }

                localRecord = localRecord.copy(
                    id = reportId,
                    localImagePath = if (remoteUrl != null) null else localRecord.localImagePath,
                    remoteThumbnailUrl = remoteUrl,
                    status = ReportStatus.QUEUED.name,
                    audioFeedback = audioFeedback
                )
                reportStore.insertOrUpdate(localRecord)

                audioFeedbackManager.speakSuccess(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Success(reportId, audioFeedback)
            } else {
                val audioFeedback = response.audioFeedback ?: when (statusCode) {
                    400 -> "Erro ao enviar. Dados de localização inválidos."
                    401 -> "Erro de autenticação no aplicativo."
                    422 -> "Falha na imagem capturada."
                    500 -> "Servidor indisponível. Tente novamente mais tarde."
                    else -> "Falha no envio. Registro salvo localmente."
                }

                localRecord = localRecord.copy(
                    status = ReportStatus.LOCAL_SAVED.name,
                    audioFeedback = audioFeedback,
                    errorMessage = response.message ?: "Status HTTP $statusCode"
                )
                reportStore.insertOrUpdate(localRecord)

                audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Failure(
                    errorMsg = response.message ?: "HTTP $statusCode",
                    audioFeedback = audioFeedback,
                    isOfflineSaved = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during report upload", e)
            val audioFeedback = "Sem conexão. Registro salvo como pendente."

            localRecord = localRecord.copy(
                status = ReportStatus.PENDING.name,
                audioFeedback = audioFeedback,
                errorMessage = e.localizedMessage ?: "Falha na conexão com o servidor"
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
            val (statusCode, response) = NativeHttpDispatcher.submitMultipartReport(
                baseUrl = settings.apiUrl,
                authToken = settings.authToken,
                imageFile = imageFile,
                latitude = localReport.latitude,
                longitude = localReport.longitude,
                accuracy = localReport.accuracy,
                timestampIso = localReport.timestamp,
                triggerType = localReport.triggerType,
                deviceId = settings.deviceId
            )

            if (statusCode in 200..299) {
                val newId = response.data?.reportId ?: localReport.id
                val audioFeedback = response.audioFeedback ?: "Registro reenviado com sucesso."

                val updatedRecord = localReport.copy(
                    id = newId,
                    status = ReportStatus.QUEUED.name,
                    audioFeedback = audioFeedback,
                    errorMessage = null
                )
                reportStore.insertOrUpdate(updatedRecord)
                audioFeedbackManager.speakSuccess(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Success(newId, audioFeedback)
            } else {
                val audioFeedback = response.audioFeedback ?: "Falha no reenvio. Registro mantido como pendente."
                val updatedRecord = localReport.copy(
                    status = ReportStatus.PENDING.name,
                    audioFeedback = audioFeedback,
                    errorMessage = response.message ?: "HTTP $statusCode"
                )
                reportStore.insertOrUpdate(updatedRecord)
                audioFeedbackManager.speakFailure(audioFeedback, settings.isAudioVoiceFeedbackEnabled)
                ReportSubmissionResult.Failure(response.message ?: "HTTP $statusCode", audioFeedback, true)
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
