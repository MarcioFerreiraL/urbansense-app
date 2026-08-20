/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GpsTextStyle
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.MinTouchTarget
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.formatTimestampShort
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.isRetryable
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.reportStatusStyle
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.triggerLabel

/** How much of an occurrence a [ReportCard] shows. */
enum class ReportCardVariant {
  /** Two-up grid tile under the map: photo on top, coordinates and time beneath. */
  COMPACT,
  /** Full-width row: photo beside the metadata, with retry and audio actions. */
  EXPANDED,
}

/**
 * The occurrence card — the single visual unit this app is really about.
 *
 * It replaces `LatestReportItemCard` (dashboard) and `ReportHistoryCard` (history), which showed
 * overlapping subsets of the same fields with different labels, different coordinate precision and
 * different status colours. The map's grid is the third consumer.
 */
@Composable
fun ReportCard(
    report: LocalReport,
    modifier: Modifier = Modifier,
    variant: ReportCardVariant = ReportCardVariant.EXPANDED,
    /** Where the report was dispatched to — the API host, shown as "Enviado para". */
    destination: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
) {
  val status = reportStatusStyle(report.status)

  // Selection is driven by the map: tapping a pin highlights its card. Animated so the change is
  // noticeable when the grid scrolls the card into view on its own.
  val containerColor by
      animateColorAsState(
          targetValue =
              if (selected) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surface,
          label = "reportCardContainer",
      )

  UsCard(
      modifier = modifier,
      onClick = onClick,
      containerColor = containerColor,
      contentPadding =
          PaddingValues(
              if (variant == ReportCardVariant.COMPACT) UrbanSenseTheme.spacing.md
              else UrbanSenseTheme.spacing.lg
          ),
  ) {
    when (variant) {
      ReportCardVariant.COMPACT -> {
        ReportThumbnail(
            localImagePath = report.localImagePath,
            remoteUrl = report.remoteThumbnailUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            onClick = onImageClick ?: onClick,
        )
        Spacer(Modifier.height(UrbanSenseTheme.spacing.sm))
        GpsText(latitude = report.latitude, longitude = report.longitude)
        TimestampRow(report.timestamp)
        Spacer(Modifier.height(UrbanSenseTheme.spacing.sm))
        UsStatusChip(
            label = status.label,
            containerColor = status.container,
            contentColor = status.content,
            icon = status.icon,
        )
      }

      ReportCardVariant.EXPANDED -> {
        Row(horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.md)) {
          ReportThumbnail(
              localImagePath = report.localImagePath,
              remoteUrl = report.remoteThumbnailUrl,
              modifier = Modifier.size(76.dp),
              onClick = onImageClick ?: onClick,
          )
          Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm),
            ) {
              UsStatusChip(
                  label = status.label,
                  containerColor = status.container,
                  contentColor = status.content,
                  icon = status.icon,
              )
              Text(
                  text = triggerLabel(report.triggerType),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.height(UrbanSenseTheme.spacing.sm))
            GpsText(latitude = report.latitude, longitude = report.longitude)
            TimestampRow(report.timestamp)
          }
        }

        val detection = report.detectionResult?.takeIf { it.isNotBlank() }
        val target = destination?.takeIf { it.isNotBlank() }
        if (detection != null || target != null) {
          Spacer(Modifier.height(UrbanSenseTheme.spacing.md))
          if (detection != null) {
            LabelledRow(stringResource(R.string.report_detection_label), detection)
          }
          if (target != null) {
            LabelledRow(stringResource(R.string.report_destination_label), target)
          }
        }

        val showRetry = onRetry != null && isRetryable(report.status)
        val showSpeak = onSpeak != null && !report.audioFeedback.isNullOrBlank()
        if (showRetry || showSpeak) {
          Spacer(Modifier.height(UrbanSenseTheme.spacing.sm))
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            if (showRetry) {
              UsSecondaryButton(
                  text = stringResource(R.string.report_retry),
                  onClick = onRetry!!,
                  icon = Icons.Default.Refresh,
                  modifier = Modifier.weight(1f),
              )
            }
            if (showSpeak) {
              IconButton(onClick = onSpeak!!, modifier = Modifier.size(MinTouchTarget)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.report_speak),
                    tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Latitude and longitude, monospaced so the digits hold their column.
 *
 * Five decimal places is about a metre of precision — enough to identify a specific pile of waste on
 * a street, and short enough to fit a two-up grid tile without truncating.
 */
@Composable
fun GpsText(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.xs),
  ) {
    Icon(
        imageVector = Icons.Default.MyLocation,
        contentDescription = stringResource(R.string.report_gps_label),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(13.dp),
    )
    Text(
        text = stringResource(R.string.report_gps_format, latitude, longitude),
        style = GpsTextStyle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun TimestampRow(isoTimestamp: String?) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.xs),
  ) {
    Icon(
        imageVector = Icons.Default.Schedule,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(13.dp),
    )
    Text(
        text = formatTimestampShort(isoTimestamp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
  }
}

@Composable
private fun LabelledRow(label: String, value: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.xs)) {
    Text(
        text = "$label:",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
