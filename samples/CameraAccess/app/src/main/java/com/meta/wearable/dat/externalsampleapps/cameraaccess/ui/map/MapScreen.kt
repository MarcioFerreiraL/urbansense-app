/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.map

import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCardVariant
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsEmptyState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.formatTimestampShort

/** Mirrors the placeholder in `secrets.defaults.properties`. */
private const val MISSING_KEY_SENTINEL = "MISSING_MAPS_API_KEY"

/** Default camera when the app has no reports and no fix yet — Recife, matching the GPS fallback. */
private val DEFAULT_CENTRE = LatLng(-8.047562, -34.877014)
private const val SINGLE_REPORT_ZOOM = 16f
private const val CITY_ZOOM = 12f

/**
 * Hues for `BitmapDescriptorFactory.defaultMarker`, chosen to track the brand palette while keeping
 * Google's own pin silhouette (which users already read as "tap me"). 132° is the hue of the logo's
 * deep green; amber and red diverge so a pending or failed report is distinguishable at a glance
 * without zooming in.
 */
private const val HUE_SENT = 132f
private const val HUE_QUEUED = 110f
private const val HUE_PENDING = 40f
private const val HUE_FAILED = 0f

/**
 * The occurrences map: every report with a valid fix as a pin, and the same reports as a two-up grid
 * of cards beneath.
 *
 * Selection is bidirectional on purpose — tapping a pin scrolls its card into view and highlights
 * it; tapping a card flies the camera to its pin. Coordinates alone are not something a person can
 * hold in their head, so the two halves have to point at each other for the screen to be usable.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val reports by viewModel.reports.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val darkTheme = isSystemInDarkTheme()

  // The key is injected at build time from local.properties. When a contributor has not set one up,
  // secrets.defaults.properties supplies MISSING_KEY_SENTINEL and the Maps SDK would render an empty
  // grey square with no explanation — so detect it and say what to do instead.
  val hasApiKey =
      remember {
        runCatching {
              val info =
                  context.packageManager.getApplicationInfo(
                      context.packageName,
                      PackageManager.GET_META_DATA,
                  )
              val key = info.metaData?.getString("com.google.android.geo.API_KEY")
              !key.isNullOrBlank() && key != MISSING_KEY_SENTINEL
            }
            .getOrDefault(false)
      }

  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(DEFAULT_CENTRE, CITY_ZOOM)
  }
  val gridState = rememberLazyGridState()
  var mapExpanded by remember { mutableStateOf(true) }
  val mapWeight by
      animateFloatAsState(targetValue = if (mapExpanded) 0.48f else 0.22f, label = "mapWeight")

  val mapStyle =
      remember(darkTheme) {
        MapStyleOptions.loadRawResourceStyle(
            context,
            if (darkTheme) R.raw.map_style_dark else R.raw.map_style,
        )
      }

  LaunchedEffect(reports.isEmpty()) {
    if (reports.isEmpty()) viewModel.resolveFallbackCentre()
  }

  // Frame everything as soon as the set of plotted points changes, so a new capture never lands
  // off-screen.
  LaunchedEffect(reports.map { it.id }) {
    when {
      reports.size >= 2 -> {
        val bounds =
            LatLngBounds.builder()
                .apply { reports.forEach { include(LatLng(it.latitude, it.longitude)) } }
                .build()
        runCatching {
          cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 96), 600)
        }
      }
      reports.size == 1 -> {
        val only = reports.first()
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(only.latitude, only.longitude),
                SINGLE_REPORT_ZOOM,
            ),
            600,
        )
      }
    }
  }

  LaunchedEffect(uiState.fallbackLatitude, uiState.fallbackLongitude) {
    val lat = uiState.fallbackLatitude
    val lng = uiState.fallbackLongitude
    if (reports.isEmpty() && lat != null && lng != null) {
      cameraPositionState.animate(
          CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), CITY_ZOOM),
          600,
      )
    }
  }

  // Pin tapped -> bring the matching card into view.
  LaunchedEffect(uiState.selectedReportId) {
    val index = reports.indexOfFirst { it.id == uiState.selectedReportId }
    if (index >= 0) {
      if (mapExpanded) mapExpanded = false
      gridState.animateScrollToItem(index)
    }
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Box(modifier = Modifier.fillMaxWidth().weight(mapWeight)) {
      if (hasApiKey) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapStyleOptions = mapStyle),
            uiSettings =
                MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
            onMapClick = { viewModel.select(null) },
        ) {
          reports.forEach { report ->
            val position = LatLng(report.latitude, report.longitude)
            Marker(
                // Held across recompositions; a fresh MarkerState on every frame would reset the
                // marker's own state (selection, info window) as the list re-emits.
                state = remember(report.id, position) { MarkerState(position = position) },
                title = formatTimestampShort(report.timestamp),
                snippet = report.detectionResult,
                icon = BitmapDescriptorFactory.defaultMarker(hueFor(report.status)),
                zIndex = if (report.id == uiState.selectedReportId) 1f else 0f,
                onClick = {
                  viewModel.select(report.id)
                  // Returning false lets the SDK also centre on the marker and show its info
                  // window, which is the behaviour people expect from a map pin.
                  false
                },
            )
          }
        }
      } else {
        MissingApiKeyPanel(modifier = Modifier.fillMaxSize())
      }

      Column(
          modifier = Modifier.align(Alignment.BottomEnd).padding(UrbanSenseTheme.spacing.md),
          verticalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm),
      ) {
        if (hasApiKey && reports.isNotEmpty()) {
          SmallFloatingActionButton(
              onClick = { viewModel.select(reports.first().id) },
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.primary,
          ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = stringResource(R.string.map_recenter),
            )
          }
        }
        SmallFloatingActionButton(
            onClick = { mapExpanded = !mapExpanded },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
          Icon(
              imageVector =
                  if (mapExpanded) Icons.Default.KeyboardArrowUp
                  else Icons.Default.KeyboardArrowDown,
              contentDescription = stringResource(R.string.map_sheet_handle),
          )
        }
      }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().weight(1f - mapWeight),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 2.dp,
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = UrbanSenseTheme.spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
          Box(
              modifier =
                  Modifier.width(36.dp)
                      .height(4.dp)
                      .background(
                          color = MaterialTheme.colorScheme.outlineVariant,
                          shape = RoundedCornerShape(percent = 50),
                      )
          )
        }

        if (reports.isEmpty()) {
          UsEmptyState(
              icon = Icons.Default.AddAPhoto,
              title = stringResource(R.string.map_empty_title),
              subtitle = stringResource(R.string.map_empty_subtitle),
              actionLabel = stringResource(R.string.map_empty_action),
              onAction = onNavigateToCapture,
          )
        } else {
          Text(
              text = stringResource(R.string.map_occurrence_count, reports.size),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier =
                  Modifier.padding(
                      start = UrbanSenseTheme.spacing.lg,
                      end = UrbanSenseTheme.spacing.lg,
                      top = UrbanSenseTheme.spacing.md,
                      bottom = UrbanSenseTheme.spacing.sm,
                  ),
          )

          LazyVerticalGrid(
              state = gridState,
              columns = GridCells.Fixed(2),
              contentPadding = PaddingValues(UrbanSenseTheme.spacing.lg),
              horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.md),
              verticalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.md),
              modifier = Modifier.fillMaxSize(),
          ) {
            items(items = reports, key = { it.id }) { report ->
              ReportCard(
                  report = report,
                  variant = ReportCardVariant.COMPACT,
                  destination = uiState.destinationHost,
                  selected = report.id == uiState.selectedReportId,
                  onClick = { viewModel.select(report.id) },
              )
            }
          }
        }
      }
    }
  }

  // Card tapped -> fly the camera to its pin. Kept separate from the scroll effect above so the two
  // directions cannot fight each other over the same LaunchedEffect key.
  val selectedPosition by
      remember(reports, uiState.selectedReportId) {
        derivedStateOf {
          reports.firstOrNull { it.id == uiState.selectedReportId }?.let {
            LatLng(it.latitude, it.longitude)
          }
        }
      }
  LaunchedEffect(selectedPosition) {
    selectedPosition?.let {
      cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, SINGLE_REPORT_ZOOM), 500)
    }
  }
}

@Composable
private fun MissingApiKeyPanel(modifier: Modifier = Modifier) {
  Box(
      modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm),
        modifier = Modifier.padding(UrbanSenseTheme.spacing.xl),
    ) {
      Icon(
          imageVector = Icons.Default.LocationOff,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(32.dp),
      )
      Text(
          text = stringResource(R.string.map_no_api_key_title),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
      )
      Text(
          text = stringResource(R.string.map_no_api_key_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
      )
    }
  }
}

private fun hueFor(rawStatus: String?): Float =
    when (runCatching { ReportStatus.valueOf(rawStatus.orEmpty()) }.getOrNull()) {
      ReportStatus.PROCESSED -> HUE_SENT
      ReportStatus.QUEUED -> HUE_QUEUED
      ReportStatus.FAILED -> HUE_FAILED
      else -> HUE_PENDING
    }
