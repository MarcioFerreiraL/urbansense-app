/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api

import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object NativeHttpDispatcher {

    private const val TAG = "UrbanSense:Http"
    private const val CONNECT_TIMEOUT_MS = 30000 // 30s connection timeout
    private const val READ_TIMEOUT_MS = 90000    // 90s timeout for YOLO11 AI inference
    private const val LINE_FEED = "\r\n"

    /**
     * Normalises whatever the user typed into "Base URL do Servidor" down to scheme+host+port,
     * then appends [path] — so a stray "/v1", "/predict" or trailing slash in Settings can't send
     * the request somewhere the backend doesn't listen.
     */
    private fun resolveTargetUrl(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return try {
            val url = URL(trimmed)
            val host = url.host
            val scheme = url.protocol
            val port = if (url.port != -1) ":${url.port}" else ""
            "$scheme://$host$port$path"
        } catch (e: Exception) {
            var clean = trimmed
            listOf("/predict", "/report", "/detect", "/reports", "/v1").forEach { suffix ->
                if (clean.endsWith(suffix)) clean = clean.substringBeforeLast(suffix)
            }
            "${clean.trimEnd('/')}$path"
        }
    }

    suspend fun submitMultipartReport(
        baseUrl: String,
        authToken: String,
        imageFile: File,
        latitude: Double,
        longitude: Double,
        accuracy: Double?,
        timestampIso: String,
        triggerType: String,
        deviceId: String?,
        toEmail: String?
    ): Pair<Int, ReportSubmissionResponse> = withContext(Dispatchers.IO) {
        val boundary = "===UrbanSenseBoundary${System.currentTimeMillis()}==="
        // /report runs detection AND e-mails the city hall server-side (urbansense-api) — the
        // Mailtrap token lives only there, never in this app. See urbansense-api/app/main.py.
        val targetUrl = resolveTargetUrl(baseUrl, "/report")
        val imageSizeKb = (imageFile.length() / 1024).toInt()

        AppLogger.net(
            TAG,
            "🚀 [POST /report] Iniciando envio de foto (${imageSizeKb} KB)\n" +
            "📍 URL: $targetUrl\n" +
            "🗺️ GPS: Lat $latitude, Lng $longitude\n" +
            "📱 DeviceId: $deviceId | Trigger: $triggerType"
        )

        var connection: HttpURLConnection? = null
        val startTime = System.currentTimeMillis()
        try {
            val url = URL(targetUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                doOutput = true
                useCaches = false
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
                if (authToken.isNotBlank()) {
                    val tokenHeader = if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken"
                    setRequestProperty("Authorization", tokenHeader)
                }
            }

            DataOutputStream(connection.outputStream).use { outputStream ->
                // 1. Text form fields (GPS & Metadata)
                writeFormField(outputStream, boundary, "latitude", latitude.toString())
                writeFormField(outputStream, boundary, "longitude", longitude.toString())
                if (accuracy != null) {
                    writeFormField(outputStream, boundary, "accuracy", accuracy.toString())
                }
                writeFormField(outputStream, boundary, "timestamp", timestampIso)
                writeFormField(outputStream, boundary, "trigger_type", triggerType)
                if (!deviceId.isNullOrBlank()) {
                    writeFormField(outputStream, boundary, "device_id", deviceId)
                }
                if (!toEmail.isNullOrBlank()) {
                    writeFormField(outputStream, boundary, "to_email", toEmail)
                }

                // 2. Binary Image File — /report's `file: UploadFile = File(...)` parameter name.
                writeFileField(outputStream, boundary, "file", imageFile)

                // End of multipart
                outputStream.writeBytes("--$boundary--$LINE_FEED")
                outputStream.flush()
            }

            val statusCode = connection.responseCode
            val durationMs = System.currentTimeMillis() - startTime
            val responseText = readStream(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

            AppLogger.net(
                TAG,
                "✅ Resposta recebida da API em ${durationMs}ms (HTTP $statusCode)\n" +
                "📄 Corpo da Resposta: $responseText"
            )

            val parsedResponse = parseSubmissionResponse(statusCode, responseText)
            Pair(statusCode, parsedResponse)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            AppLogger.e(
                TAG,
                "❌ Erro de conexão com a API ($targetUrl) após ${durationMs}ms: ${e.localizedMessage}",
                e
            )
            Pair(
                -1,
                ReportSubmissionResponse(
                    status = "error",
                    code = "NETWORK_ERROR",
                    message = e.localizedMessage ?: "Sem conexão de rede",
                    audioFeedback = "Erro de conexão. Registro salvo localmente."
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchReportsHistory(
        baseUrl: String,
        authToken: String,
        page: Int = 1,
        limit: Int = 20
    ): Pair<Int, ReportHistoryResponse> = withContext(Dispatchers.IO) {
        val targetUrl = if (baseUrl.endsWith("/")) "${baseUrl}reports?page=$page&limit=$limit" else "$baseUrl/reports?page=$page&limit=$limit"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                setRequestProperty("Accept", "application/json")
                val tokenHeader = if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken"
                setRequestProperty("Authorization", tokenHeader)
            }

            val statusCode = connection.responseCode
            val responseText = readStream(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

            val parsed = parseHistoryResponse(responseText)
            Pair(statusCode, parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote history", e)
            Pair(-1, ReportHistoryResponse(status = "error", total = 0, data = emptyList()))
        } finally {
            connection?.disconnect()
        }
    }

    private fun writeFormField(output: DataOutputStream, boundary: String, fieldName: String, value: String) {
        output.writeBytes("--$boundary$LINE_FEED")
        output.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"$LINE_FEED$LINE_FEED")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeBytes(LINE_FEED)
    }

    private fun writeFileField(output: DataOutputStream, boundary: String, fieldName: String, file: File) {
        output.writeBytes("--$boundary$LINE_FEED")
        output.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"$LINE_FEED")
        output.writeBytes("Content-Type: image/jpeg$LINE_FEED$LINE_FEED")

        FileInputStream(file).use { input ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
        }
        output.writeBytes(LINE_FEED)
    }

    private fun readStream(inputStream: java.io.InputStream?): String {
        if (inputStream == null) return ""
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            sb.toString()
        }
    }

    private fun parseSubmissionResponse(statusCode: Int, jsonString: String): ReportSubmissionResponse {
        if (jsonString.isBlank()) {
            val defaultFeedback = when (statusCode) {
                201 -> "Registro enviado com sucesso."
                400 -> "Erro ao enviar. Dados de localização inválidos."
                401 -> "Erro de autenticação no aplicativo."
                422 -> "Falha na imagem capturada."
                500 -> "Servidor indisponível. Tente novamente mais tarde."
                else -> "Falha no envio. Registro salvo localmente."
            }
            return ReportSubmissionResponse(
                status = if (statusCode in 200..299) "success" else "error",
                audioFeedback = defaultFeedback
            )
        }

        return try {
            val json = JSONObject(jsonString)

            // Check if response comes from urbansense-api (FastAPI YOLO11 model)
            val isYoloFastApi = json.has("has_trash") || json.has("has_pothole") || json.has("detections")

            if (isYoloFastApi) {
                val hasTrash = json.optBoolean("has_trash", false)
                val hasPothole = json.optBoolean("has_pothole", false)

                val detectionLabel = when {
                    hasTrash && hasPothole -> "DESCARTE DE LIXO E BURACO NA VIA"
                    hasTrash -> "DESCARTE DE LIXO DETECTADO"
                    hasPothole -> "BURACO NA VIA DETECTADO"
                    else -> "SEM RESIDUOS"
                }

                val dynamicAudioFeedback = when {
                    hasTrash && hasPothole -> "Atenção: Descarte irregular de lixo e buraco na pista detectados!"
                    hasTrash -> "Descarte irregular de lixo identificado na via."
                    hasPothole -> "Atenção: Buraco na pista detectado."
                    else -> "Vistoria concluída. Nenhum problema urbano detectado."
                }

                val reportId = "pred_" + UUID.randomUUID().toString().take(8)
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())

                val reportData = ReportData(
                    reportId = reportId,
                    createdAt = timestamp,
                    status = "PROCESSED",
                    thumbnailUrl = null,
                    audioFeedback = dynamicAudioFeedback,
                    detectionResult = detectionLabel
                )

                return ReportSubmissionResponse(
                    status = "success",
                    message = "Incerence completed",
                    code = "200",
                    audioFeedback = dynamicAudioFeedback,
                    detectionResult = detectionLabel,
                    data = reportData
                )
            }

            val status = json.optString("status", "unknown")
            val message = if (json.has("message") && !json.isNull("message")) json.getString("message") else null
            val code = if (json.has("code") && !json.isNull("code")) json.getString("code") else null
            val audioFeedback = if (json.has("audio_feedback") && !json.isNull("audio_feedback")) json.getString("audio_feedback") else null
            val detectionResult = if (json.has("detection_result") && !json.isNull("detection_result")) json.getString("detection_result") else null

            var reportData: ReportData? = null
            if (json.has("data") && !json.isNull("data")) {
                val dataObj = json.getJSONObject("data")
                reportData = ReportData(
                    reportId = dataObj.optString("report_id", UUID.randomUUID().toString()),
                    createdAt = dataObj.optString("created_at", ""),
                    status = dataObj.optString("status", "QUEUED"),
                    thumbnailUrl = if (dataObj.has("thumbnail_url") && !dataObj.isNull("thumbnail_url")) dataObj.getString("thumbnail_url") else null,
                    audioFeedback = if (dataObj.has("audio_feedback") && !dataObj.isNull("audio_feedback")) dataObj.getString("audio_feedback") else null,
                    detectionResult = if (dataObj.has("detection_result") && !dataObj.isNull("detection_result")) dataObj.getString("detection_result") else null
                )
            }

            val finalAudioFeedback = audioFeedback ?: reportData?.audioFeedback ?: when (statusCode) {
                200, 201 -> "Registro enviado com sucesso."
                400 -> "Erro ao enviar. Dados de localização inválidos."
                401 -> "Erro de autenticação no aplicativo."
                422 -> "Falha na imagem capturada."
                500 -> "Servidor indisponível. Tente novamente mais tarde."
                else -> "Falha no envio. Registro salvo localmente."
            }

            ReportSubmissionResponse(
                status = status,
                message = message,
                code = code,
                audioFeedback = finalAudioFeedback,
                detectionResult = detectionResult ?: reportData?.detectionResult,
                data = reportData
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse error on submission response", e)
            ReportSubmissionResponse(
                status = "error",
                audioFeedback = "Falha no envio. Registro salvo localmente."
            )
        }
    }

    private fun parseHistoryResponse(jsonString: String): ReportHistoryResponse {
        if (jsonString.isBlank()) return ReportHistoryResponse(status = "error")
        return try {
            val json = JSONObject(jsonString)
            val status = json.optString("status", "success")
            val page = json.optInt("page", 1)
            val limit = json.optInt("limit", 20)
            val total = json.optInt("total", 0)

            val items = mutableListOf<ReportItem>()
            if (json.has("data")) {
                val dataArray = json.getJSONArray("data")
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    items.add(
                        ReportItem(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            thumbnailUrl = if (item.has("thumbnail_url") && !item.isNull("thumbnail_url")) item.getString("thumbnail_url") else null,
                            latitude = item.optDouble("latitude", 0.0),
                            longitude = item.optDouble("longitude", 0.0),
                            accuracy = if (item.has("accuracy")) item.optDouble("accuracy") else null,
                            triggerType = item.optString("trigger_type", "AUTOMATIC"),
                            status = item.optString("status", "PROCESSED"),
                            detectionResult = if (item.has("detection_result") && !item.isNull("detection_result")) item.getString("detection_result") else null,
                            createdAt = item.optString("created_at", "")
                        )
                    )
                }
            }

            ReportHistoryResponse(
                status = status,
                page = page,
                limit = limit,
                total = total,
                data = items
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse error on history response", e)
            ReportHistoryResponse(status = "error")
        }
    }
}
