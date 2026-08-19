/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.TriggerType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Single source of truth for how a report's status looks and reads.
 *
 * This mapping used to exist in three places at once — the dashboard's latest-report badge, the
 * history chip, and the history detail dialog — each with its own colours and its own wording, so
 * the same report could read "Enviado" in one screen and "Enviado com sucesso" in another, in a
 * different green. The map screen adds a fourth consumer, which is what made the duplication worth
 * collapsing.
 */
@Immutable
data class ReportStatusStyle(
    /** Compact wording, for a chip. */
    val label: String,
    /** Full sentence, for a detail row. */
    val longLabel: String,
    val container: Color,
    val content: Color,
    /** Colour of this status' pin on the map. */
    val pin: Color,
    val icon: ImageVector,
)

/** Resolves [rawStatus] (as persisted by `ReportRepository`) into presentation tokens. */
@Composable
fun reportStatusStyle(rawStatus: String?): ReportStatusStyle {
  val semantic = UrbanSenseTheme.semantic
  val status = runCatching { ReportStatus.valueOf(rawStatus.orEmpty()) }.getOrNull()

  return when (status) {
    ReportStatus.PENDING ->
        ReportStatusStyle(
            label = stringResource(R.string.report_status_pending),
            longLabel = stringResource(R.string.report_status_pending_long),
            container = semantic.warningContainer,
            content = semantic.onWarningContainer,
            pin = semantic.pinPending,
            icon = Icons.Default.Schedule,
        )
    ReportStatus.QUEUED,
    ReportStatus.PROCESSED ->
        ReportStatusStyle(
            label = stringResource(R.string.report_status_sent),
            longLabel = stringResource(R.string.report_status_sent_long),
            container = semantic.successContainer,
            content = semantic.onSuccessContainer,
            pin =
                if (status == ReportStatus.PROCESSED) semantic.pinProcessed else semantic.pinQueued,
            icon = Icons.Default.CheckCircle,
        )
    ReportStatus.FAILED ->
        ReportStatusStyle(
            label = stringResource(R.string.report_status_failed),
            longLabel = stringResource(R.string.report_status_failed_long),
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            pin = semantic.pinFailed,
            icon = Icons.Default.ErrorOutline,
        )
    ReportStatus.LOCAL_SAVED ->
        ReportStatusStyle(
            label = stringResource(R.string.report_status_local),
            longLabel = stringResource(R.string.report_status_local_long),
            container = semantic.infoContainer,
            content = semantic.onInfoContainer,
            pin = semantic.pinPending,
            icon = Icons.Default.CloudOff,
        )
    // An unrecognised status previously fell through to a branch that printed the raw enum name
    // into the chip, which is how strings like "LOCAL_SAVED" leaked into the UI.
    null ->
        ReportStatusStyle(
            label = stringResource(R.string.report_status_unknown),
            longLabel = stringResource(R.string.report_status_unknown),
            container = semantic.infoContainer,
            content = semantic.onInfoContainer,
            pin = semantic.pinPending,
            icon = Icons.Default.HelpOutline,
        )
  }
}

/** True when the report can still be retried by the user. */
fun isRetryable(rawStatus: String?): Boolean =
    rawStatus == ReportStatus.PENDING.name ||
        rawStatus == ReportStatus.FAILED.name ||
        rawStatus == ReportStatus.LOCAL_SAVED.name

/** Human label for how the capture was triggered. */
@Composable
fun triggerLabel(rawTrigger: String?): String =
    if (rawTrigger == TriggerType.AUTOMATIC.name) {
      stringResource(R.string.report_trigger_automatic)
    } else {
      stringResource(R.string.report_trigger_manual)
    }

private val DAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

/**
 * Renders the ISO-8601 UTC timestamp the API contract mandates into the phone's local time zone.
 *
 * Screens previously printed `report.timestamp` verbatim, so the user saw `2026-08-17T13:40:02Z` —
 * UTC, and unreadable at a glance while walking down a street.
 */
fun formatTimestampShort(isoTimestamp: String?): String = formatTimestamp(isoTimestamp, DAY_TIME)

fun formatTimestampFull(isoTimestamp: String?): String = formatTimestamp(isoTimestamp, FULL)

private fun formatTimestamp(isoTimestamp: String?, formatter: DateTimeFormatter): String {
  if (isoTimestamp.isNullOrBlank()) return "—"
  return try {
    Instant.parse(isoTimestamp).atZone(ZoneId.systemDefault()).format(formatter)
  } catch (e: DateTimeParseException) {
    // Older rows, or anything the server sent in an unexpected shape, are shown as-is rather than
    // blanked — an odd-looking timestamp is more debuggable than an empty field.
    isoTimestamp
  }
}

/**
 * True when the report carries a usable fix. `LocalReport.latitude`/`longitude` are non-null
 * primitives that default to 0.0 when the GPS timed out, and (0, 0) is a real point in the Atlantic
 * — plotting it would drop a pin off the coast of Africa for every failed fix.
 */
fun hasValidLocation(latitude: Double, longitude: Double): Boolean =
    latitude != 0.0 &&
        longitude != 0.0 &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0
