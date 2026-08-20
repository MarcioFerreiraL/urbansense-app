/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.email

import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.MailtrapEmailDispatcher
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Builds the HTML e-mail sent to the city hall for every occurrence — table-based markup only
 * (no flexbox/grid) because e-mail clients render HTML with a 2005-era engine, not a browser one.
 *
 * Visual identity matches the app: [Brand.GreenDeep]/[Brand.GreenAction] on white, the same palette
 * `ui/theme/Color.kt` samples from the UrbanSense logo — kept as literal hex here since an e-mail
 * template can't reach into Compose theme tokens.
 */
object ReportEmailTemplate {

    private const val GREEN_DEEP = "#295C33"
    private const val GREEN_ACTION = "#37703F"
    private const val GREEN_SOFT_BG = "#E6F1E1"
    private const val TEXT_MUTED = "#5B6B5E"

    private val FULL_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", Locale("pt", "BR"))

    data class ReportEmailData(
        val reportId: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double?,
        val timestampIso: String?,
        val triggerLabel: String,
        val deviceId: String?,
        val cityHallName: String,
    )

    fun subject(data: ReportEmailData): String =
        "UrbanSense AI — Nova ocorrência de descarte irregular detectada"

    fun buildHtml(data: ReportEmailData): String {
        val mapsUrl = "https://www.openstreetmap.org/?mlat=${data.latitude}&mlon=${data.longitude}#map=18/${data.latitude}/${data.longitude}"
        val gps = "%.5f, %.5f".format(Locale.US, data.latitude, data.longitude)
        val accuracyText = data.accuracy?.let { "±%.0f m".format(Locale.US, it) } ?: "—"
        val whenText = formatIso(data.timestampIso)

        return """
        <!doctype html>
        <html lang="pt-BR">
        <body style="margin:0;padding:0;background:#F2F6F1;font-family:Helvetica,Arial,sans-serif;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F2F6F1;padding:24px 0;">
            <tr><td align="center">
              <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:16px;overflow:hidden;border:1px solid #E1EAE0;">

                <tr>
                  <td style="background:$GREEN_DEEP;padding:24px 28px;">
                    <span style="color:#FFFFFF;font-size:20px;font-weight:bold;letter-spacing:0.3px;">UrbanSense AI</span><br/>
                    <span style="color:#CFE3CC;font-size:13px;">Monitoramento urbano via Meta Ray-Ban</span>
                  </td>
                </tr>

                <tr>
                  <td style="padding:24px 28px 8px 28px;">
                    <span style="display:inline-block;background:$GREEN_SOFT_BG;color:$GREEN_ACTION;font-size:12px;font-weight:bold;padding:6px 12px;border-radius:999px;">
                      DESCARTE IRREGULAR DETECTADO
                    </span>
                    <h1 style="margin:14px 0 4px 0;font-size:19px;color:#1E2A20;">Nova ocorrência registrada por um cidadão</h1>
                    <p style="margin:0;font-size:14px;color:$TEXT_MUTED;line-height:1.5;">
                      Um usuário do aplicativo UrbanSense AI flagrou e enviou o registro abaixo à
                      <b>${escape(data.cityHallName)}</b> para providências.
                    </p>
                  </td>
                </tr>

                <tr>
                  <td style="padding:12px 28px;">
                    <img src="cid:${MailtrapEmailDispatcher.REPORT_PHOTO_CID}" alt="Foto da ocorrência"
                         width="504" style="width:100%;max-width:504px;border-radius:12px;border:1px solid #E1EAE0;display:block;" />
                  </td>
                </tr>

                <tr>
                  <td style="padding:8px 28px 24px 28px;">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                      ${metadataRow("Data e hora", whenText)}
                      ${metadataRow("Coordenadas GPS", gps)}
                      ${metadataRow("Precisão do sinal", accuracyText)}
                      ${metadataRow("Tipo de captura", data.triggerLabel)}
                      ${metadataRow("Dispositivo", data.deviceId ?: "—")}
                      ${metadataRow("ID do registro", data.reportId)}
                    </table>
                    <a href="$mapsUrl" style="display:inline-block;margin-top:16px;background:$GREEN_ACTION;color:#FFFFFF;text-decoration:none;font-size:14px;font-weight:bold;padding:12px 20px;border-radius:10px;">
                      Ver localização no mapa
                    </a>
                  </td>
                </tr>

                <tr>
                  <td style="background:$GREEN_SOFT_BG;padding:16px 28px;">
                    <p style="margin:0;font-size:12px;color:$GREEN_ACTION;line-height:1.5;">
                      E-mail gerado automaticamente pelo UrbanSense AI a partir de uma ocorrência capturada em campo.
                      Não é necessário responder a esta mensagem.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.trimIndent()
    }

    private fun metadataRow(label: String, value: String): String = """
        <tr>
          <td style="padding:7px 0;font-size:13px;color:$TEXT_MUTED;width:150px;border-bottom:1px solid #EEF3ED;">${escape(label)}</td>
          <td style="padding:7px 0;font-size:13px;color:#1E2A20;font-weight:bold;border-bottom:1px solid #EEF3ED;">${escape(value)}</td>
        </tr>
    """.trimIndent()

    private fun formatIso(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return "—"
        return try {
            Instant.parse(isoTimestamp).atZone(ZoneId.systemDefault()).format(FULL_DATE)
        } catch (e: DateTimeParseException) {
            isoTimestamp
        }
    }

    private fun escape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
