/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends the occurrence notification e-mail through the Mailtrap HTTP Send API.
 *
 * A plain HTTPS POST rather than SMTP: no `javax.mail`/`Session` dependency, and it reuses the same
 * `HttpURLConnection` + `org.json` style as [NativeHttpDispatcher] — one way of talking to the
 * network in this codebase, not two.
 */
object MailtrapEmailDispatcher {

    private const val TAG = "UrbanSense:Mail"
    private const val TIMEOUT_MS = 20000

    /** Content-ID the photo is attached under — [ReportEmailTemplate] references it as `cid:$REPORT_PHOTO_CID`. */
    const val REPORT_PHOTO_CID = "report-photo"

    data class EmailResult(val success: Boolean, val statusCode: Int, val message: String?)

    suspend fun sendReportEmail(
        apiUrl: String,
        apiToken: String,
        fromEmail: String,
        toEmail: String,
        subject: String,
        htmlBody: String,
        imageFile: File?,
    ): EmailResult = withContext(Dispatchers.IO) {
        if (apiToken.isBlank() || toEmail.isBlank()) {
            return@withContext EmailResult(
                success = false,
                statusCode = -1,
                message = "Token do Mailtrap ou e-mail da prefeitura não configurado",
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val payload = JSONObject().apply {
                put("from", JSONObject().apply {
                    put("email", fromEmail)
                    put("name", "UrbanSense AI")
                })
                put("to", JSONArray().put(JSONObject().put("email", toEmail)))
                put("subject", subject)
                put("html", htmlBody)
                put("category", "UrbanSense - Ocorrência")
                if (imageFile != null && imageFile.exists()) {
                    val encoded = Base64.encodeToString(imageFile.readBytes(), Base64.NO_WRAP)
                    put("attachments", JSONArray().put(JSONObject().apply {
                        put("content", encoded)
                        put("filename", imageFile.name)
                        put("type", "image/jpeg")
                        // Inline (not a plain attachment) so the HTML body can show the photo itself
                        // via `cid:report-photo` — see ReportEmailTemplate.
                        put("disposition", "inline")
                        put("content_id", REPORT_PHOTO_CID)
                    }))
                }
            }

            val connectionUrl = URL(apiUrl)
            connection = (connectionUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Api-Token", apiToken)
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val statusCode = connection.responseCode
            val responseText = readStream(if (statusCode in 200..299) connection.inputStream else connection.errorStream)
            Log.d(TAG, "Mailtrap response ($statusCode): $responseText")

            EmailResult(
                success = statusCode in 200..299,
                statusCode = statusCode,
                message = if (statusCode in 200..299) null else responseText.ifBlank { "HTTP $statusCode" },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send report e-mail via Mailtrap", e)
            EmailResult(success = false, statusCode = -1, message = e.localizedMessage ?: "Erro de conexão com o Mailtrap")
        } finally {
            connection?.disconnect()
        }
    }

    private fun readStream(inputStream: java.io.InputStream?): String {
        if (inputStream == null) return ""
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
