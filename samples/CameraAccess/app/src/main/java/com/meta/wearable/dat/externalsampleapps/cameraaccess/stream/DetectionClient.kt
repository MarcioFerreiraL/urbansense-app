/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// DetectionClient - sends a captured photo to the local garbage-detection server
// (see garbage-detection-training/serve.py at the repo root) and returns the same
// photo annotated with detection boxes. Falls back to null (caller keeps the
// original photo) if the server can't be reached, so a demo never breaks just
// because the laptop isn't on the network.

package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** The photo with detection boxes drawn, plus a short human-readable summary of what was found. */
data class DetectionResult(val bitmap: Bitmap, val message: String)

object DetectionClient {
  private const val TAG = "DetectionClient"

  // Troque pelo IP local do computador rodando serve.py (mesma rede Wi-Fi do
  // celular). Descubra com "ipconfig" no PC, procurando "Endereço IPv4".
  private const val SERVER_URL = "http://192.168.2.15:8000/detect"

  private val client = OkHttpClient()

  /** Retorna a foto anotada + a mensagem, ou null se o servidor não respondeu. */
  suspend fun detect(bitmap: Bitmap): DetectionResult? =
      withContext(Dispatchers.IO) {
        try {
          val jpegBytes =
              ByteArrayOutputStream()
                  .also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                  .toByteArray()

          val requestBody =
              MultipartBody.Builder()
                  .setType(MultipartBody.FORM)
                  .addFormDataPart(
                      "image",
                      "photo.jpg",
                      jpegBytes.toRequestBody("image/jpeg".toMediaType()),
                  )
                  .build()

          val request = Request.Builder().url(SERVER_URL).post(requestBody).build()

          client.newCall(request).execute().use { response ->
            Log.d(TAG, "Resposta do servidor: HTTP ${response.code}")
            if (!response.isSuccessful) {
              Log.e(TAG, "Servidor respondeu com erro: HTTP ${response.code} - ${response.body?.string()}")
              return@withContext null
            }
            val body = response.body ?: return@withContext null
            val annotatedBitmap = BitmapFactory.decodeStream(body.byteStream()) ?: return@withContext null
            val detectionsHeader = response.header("X-Detections")
            DetectionResult(bitmap = annotatedBitmap, message = formatMessage(detectionsHeader))
          }
        } catch (e: Exception) {
          Log.e(TAG, "Falha ao chamar o servidor de detecção em $SERVER_URL", e)
          null
        }
      }

  // O servidor manda "none" (nada encontrado) ou algo como "Trash:0.92,pothole:0.55"
  // (uma detecção por vírgula, classe:confiança). Isso vira uma frase simples, do
  // jeito que o app mostraria pro usuário (ou leria em voz alta, futuramente).
  private fun formatMessage(detectionsHeader: String?): String {
    if (detectionsHeader.isNullOrBlank() || detectionsHeader == "none") {
      return "Nenhuma ocorrência identificada."
    }
    val descriptions =
        detectionsHeader.split(",").mapNotNull { entry ->
          val (className, confidence) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
          val percent = (confidence.toFloatOrNull()?.times(100))?.toInt() ?: return@mapNotNull null
          "$className identificado ($percent% de confiança)"
        }
    return descriptions.joinToString(separator = "\n").ifEmpty { "Nenhuma ocorrência identificada." }
  }
}
