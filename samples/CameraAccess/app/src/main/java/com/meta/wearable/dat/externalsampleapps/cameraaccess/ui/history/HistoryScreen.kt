/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.GpsText
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.FullscreenPhotoViewerDialog
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportThumbnail
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsEmptyState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsSecondaryButton
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsSectionHeader
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsStatusChip
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.formatTimestampFull
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.hasValidLocation
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.isRetryable
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.openExternalMap
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.reportStatusStyle
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.triggerLabel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val reports by viewModel.reports.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val spacing = UrbanSenseTheme.spacing

  var showClearDialog by remember { mutableStateOf(false) }
  var fullScreenReport by remember { mutableStateOf<LocalReport?>(null) }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(horizontal = spacing.lg)
              .padding(top = spacing.lg)
  ) {
    UsSectionHeader(
        title = stringResource(R.string.history_title),
        subtitle = stringResource(R.string.map_occurrence_count, reports.size),
        trailing = {
          IconButton(onClick = { viewModel.refreshFromApi() }, enabled = !uiState.isSyncing) {
            if (uiState.isSyncing) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = stringResource(R.string.history_sync),
                  tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          if (reports.isNotEmpty()) {
            IconButton(onClick = { showClearDialog = true }) {
              Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = stringResource(R.string.history_clear),
                  tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
    )

    uiState.syncErrorMessage?.let { message ->
      Spacer(Modifier.height(spacing.sm))
      Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(spacing.md),
        )
      }
    }

    Spacer(Modifier.height(spacing.lg))

    if (reports.isEmpty()) {
      Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        UsEmptyState(
            icon = Icons.Default.History,
            title = stringResource(R.string.history_empty_title),
            subtitle = stringResource(R.string.history_empty_subtitle),
        )
      }
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          verticalArrangement = Arrangement.spacedBy(spacing.md),
          contentPadding = PaddingValues(bottom = spacing.lg),
      ) {
        items(reports, key = { it.id }) { report ->
          ReportCard(
              report = report,
              modifier = Modifier.fillMaxWidth(),
              onClick = { viewModel.selectReport(report) },
              onImageClick = { fullScreenReport = report },
              onRetry = { viewModel.retryReport(report.id) },
              onSpeak = { report.audioFeedback?.let(onSpeak) },
          )
        }
      }
    }
  }

  uiState.selectedReport?.let { report ->
    ReportDetailDialog(
        report = report,
        isSyncing = uiState.isSyncing,
        onDismiss = { viewModel.selectReport(null) },
        onOpenMap = {
          openExternalMap(
              context = context,
              latitude = report.latitude,
              longitude = report.longitude,
              label = context.getString(R.string.report_map_pin_label),
          )
        },
        onRetry = { viewModel.retryReport(report.id) },
        onImageClick = { fullScreenReport = it },
    )
  }

  if (showClearDialog) {
    AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text(stringResource(R.string.history_clear)) },
        text = { Text(stringResource(R.string.history_clear_confirm)) },
        confirmButton = {
          TextButton(
              onClick = {
                viewModel.clearAllHistory()
                showClearDialog = false
              }
          ) {
            Text(
                stringResource(R.string.history_clear_action),
                color = MaterialTheme.colorScheme.error,
            )
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearDialog = false }) {
            Text(stringResource(R.string.camera_permission_cancel))
          }
        },
    )
  }

  fullScreenReport?.let { report ->
      FullscreenPhotoViewerDialog(
          localImagePath = report.localImagePath,
          remoteUrl = report.remoteThumbnailUrl,
          title = "Foto da Ocorrência #${report.id.take(8)}",
          onDismissRequest = { fullScreenReport = null }
      )
  }
}

@Composable
private fun ReportDetailDialog(
    report: LocalReport,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onOpenMap: () -> Unit,
    onRetry: () -> Unit,
    onImageClick: (LocalReport) -> Unit = {},
) {
  val spacing = UrbanSenseTheme.spacing
  val status = reportStatusStyle(report.status)
  val canOpenMap = hasValidLocation(report.latitude, report.longitude)

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.history_detail_title)) },
      text = {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
          ReportThumbnail(
              localImagePath = report.localImagePath,
              remoteUrl = report.remoteThumbnailUrl,
              modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
              shape = MaterialTheme.shapes.medium,
              onClick = { onImageClick(report) },
          )

          UsStatusChip(
              label = status.longLabel,
              containerColor = status.container,
              contentColor = status.content,
              icon = status.icon,
          )

          GpsText(latitude = report.latitude, longitude = report.longitude)
          report.accuracy?.let {
            DetailRow(stringResource(R.string.history_detail_accuracy), stringResource(R.string.report_accuracy_format, it))
          }
          DetailRow(
              stringResource(R.string.history_detail_timestamp),
              formatTimestampFull(report.timestamp),
          )
          DetailRow(
              stringResource(R.string.history_detail_trigger),
              triggerLabel(report.triggerType),
          )
          report.detectionResult?.takeIf { it.isNotBlank() }?.let {
            DetailRow(stringResource(R.string.report_detection_label), it)
          }
          report.errorMessage?.takeIf { it.isNotBlank() }?.let {
            DetailRow(stringResource(R.string.history_detail_error), it)
          }

          if (canOpenMap) {
            UsSecondaryButton(
                text = stringResource(R.string.report_open_external_map),
                onClick = onOpenMap,
                icon = Icons.Default.Map,
                modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        if (isRetryable(report.status)) {
          TextButton(onClick = onRetry, enabled = !isSyncing) {
            Text(stringResource(R.string.report_retry))
          }
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_preview)) }
      },
  )
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm)) {
    Text(
        text = "$label:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
