/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionActivityType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsSectionHeader
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsStatTile
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.MinTouchTarget
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val motionStatus by viewModel.motionStatus.collectAsState()
  val captureState by viewModel.captureState.collectAsState()
  val settings by viewModel.settings.collectAsState()
  val latestReport by viewModel.latestReport.collectAsState()
  val totalReports by viewModel.totalReportsCount.collectAsState()
  val wearablesState by viewModel.wearablesViewModel.uiState.collectAsState()

  val spacing = UrbanSenseTheme.spacing
  val scrollState = rememberScrollState()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .verticalScroll(scrollState)
              .padding(spacing.lg),
      verticalArrangement = Arrangement.spacedBy(spacing.lg),
  ) {
    UsSectionHeader(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.app_tagline),
        trailing = { ConnectionChip(isConnected = wearablesState.hasActiveDevice, onClick = onNavigateToCamera) },
    )

    MotionCard(
        activityType = motionStatus.activityType,
        isMoving = motionStatus.isMoving,
        isSleepMode = captureState.isSleepMode,
        onSimulate = viewModel::simulateMotion,
    )

    AutoCaptureCard(
        isActive = captureState.isAutoCaptureActive,
        intervalSec = settings.autoCaptureIntervalSec,
        countdownSec = captureState.countdownSec,
        onToggle = viewModel::toggleAutoCapture,
    )

    ManualCaptureButton(
        isCapturing = captureState.isCapturingNow,
        onCapture = viewModel::triggerManualCapture,
    )

    latestReport?.let { report ->
      Text(
          text = stringResource(R.string.dashboard_latest_report),
          style = MaterialTheme.typography.titleMedium,
      )
      ReportCard(
          report = report,
          modifier = Modifier.fillMaxWidth(),
          onSpeak = { viewModel.testAudioFeedback() },
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
      UsStatTile(
          value = "$totalReports",
          label = stringResource(R.string.dashboard_total_reports),
          icon = Icons.Default.Send,
          modifier = Modifier.weight(1f),
      )
      UsStatTile(
          value = "${captureState.totalCapturesInSession}",
          label = stringResource(R.string.dashboard_session_captures),
          icon = Icons.Default.Timeline,
          modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun ConnectionChip(isConnected: Boolean, onClick: () -> Unit) {
  // Connected reads as the brand green; disconnected stays neutral rather than red — not having the
  // glasses on is a normal resting state, not an error.
  val container =
      if (isConnected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant
  val content =
      if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
      shape = RoundedCornerShape(percent = 50),
      color = container,
      modifier = Modifier.clickable(onClick = onClick),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(content))
      Text(
          text =
              stringResource(
                  if (isConnected) R.string.dashboard_glasses_connected
                  else R.string.dashboard_glasses_disconnected
              ),
          style = MaterialTheme.typography.labelSmall,
          color = content,
      )
    }
  }
}

@Composable
private fun MotionCard(
    activityType: MotionActivityType,
    isMoving: Boolean,
    isSleepMode: Boolean,
    onSimulate: (MotionActivityType) -> Unit,
) {
  val spacing = UrbanSenseTheme.spacing
  val semantic = UrbanSenseTheme.semantic

  // The pulse is the only signal that motion detection is live; without it a stationary user cannot
  // tell the difference between "asleep by design" and "crashed".
  val transition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by
      transition.animateFloat(
          initialValue = 1f,
          targetValue = if (isMoving) 1.08f else 1f,
          animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
          label = "scale",
      )

  val accent = if (isMoving) MaterialTheme.colorScheme.primary else semantic.warning

  UsCard(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(spacing.sm),
          modifier = Modifier.weight(1f),
      ) {
        Box(
            modifier =
                Modifier.size(38.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
          Icon(
              imageVector =
                  when (activityType) {
                    MotionActivityType.WALKING -> Icons.Default.DirectionsWalk
                    MotionActivityType.RUNNING -> Icons.Default.DirectionsRun
                    MotionActivityType.ON_BICYCLE -> Icons.Default.DirectionsBike
                    MotionActivityType.IN_VEHICLE -> Icons.Default.Bolt
                    else -> Icons.Default.NightlightRound
                  },
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(20.dp),
          )
        }

        Column {
          Text(
              text = stringResource(R.string.dashboard_motion_title),
              style = MaterialTheme.typography.titleMedium,
          )
          Text(
              text = stringResource(activityType.labelRes),
              style = MaterialTheme.typography.bodyMedium,
              color = accent,
          )
        }
      }

      Surface(
          shape = RoundedCornerShape(percent = 50),
          color =
              if (isSleepMode) MaterialTheme.colorScheme.surfaceVariant
              else MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
            text =
                stringResource(
                    if (isSleepMode) R.string.dashboard_sleep_mode else R.string.dashboard_active_mode
                ),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isSleepMode) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }

    Spacer(Modifier.height(spacing.md))
    Text(
        text =
            stringResource(
                if (isSleepMode) R.string.dashboard_sleep_explainer
                else R.string.dashboard_active_explainer
            ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(spacing.md))
    Text(
        text = stringResource(R.string.dashboard_simulate_label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(spacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
      listOf(
              MotionActivityType.STILL,
              MotionActivityType.WALKING,
              MotionActivityType.RUNNING,
          )
          .forEach { type ->
            FilledTonalButton(
                onClick = { onSimulate(type) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
              Text(
                  text = stringResource(type.labelRes),
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  textAlign = TextAlign.Center,
              )
            }
          }
    }
  }
}

@Composable
private fun AutoCaptureCard(
    isActive: Boolean,
    intervalSec: Int,
    countdownSec: Int,
    onToggle: () -> Unit,
) {
  val spacing = UrbanSenseTheme.spacing

  UsCard(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = stringResource(R.string.dashboard_auto_capture_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.dashboard_auto_capture_interval, intervalSec),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(checked = isActive, onCheckedChange = { onToggle() })
    }

    if (countdownSec > 0) {
      Spacer(Modifier.height(spacing.sm))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(spacing.sm),
      ) {
        CircularProgressIndicator(
            progress = { countdownSec.toFloat() / intervalSec.coerceAtLeast(1) },
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.dashboard_next_capture, countdownSec),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun ManualCaptureButton(isCapturing: Boolean, onCapture: () -> Unit) {
  Button(
      onClick = onCapture,
      enabled = !isCapturing,
      modifier = Modifier.fillMaxWidth().height(MinTouchTarget + 16.dp),
      shape = MaterialTheme.shapes.medium,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    if (isCapturing) {
      CircularProgressIndicator(
          color = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(22.dp),
          strokeWidth = 3.dp,
      )
    } else {
      Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
      )
    }
    Spacer(Modifier.width(UrbanSenseTheme.spacing.md))
    Text(
        text =
            stringResource(
                if (isCapturing) R.string.dashboard_capturing else R.string.dashboard_manual_capture
            ),
        style = MaterialTheme.typography.titleMedium,
    )
  }
}
