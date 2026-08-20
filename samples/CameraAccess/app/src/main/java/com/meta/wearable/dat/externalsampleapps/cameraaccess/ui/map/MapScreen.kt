/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.ReportStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportCardVariant
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.UsEmptyState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.Brand
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay

/** Default camera when the app has no reports and no fix yet — Recife, matching the GPS fallback. */
private val DEFAULT_CENTRE = GeoPoint(-8.047562, -34.877014)
private const val SINGLE_REPORT_ZOOM = 17.0
private const val CITY_ZOOM = 13.0

/**
 * The occurrences map: every report with a valid fix as a pin, and the same reports as a two-up grid
 * of cards beneath.
 *
 * Runs on OpenStreetMap tiles (osmdroid) rather than Google Maps: no API key, no Google Cloud billing
 * account, and no usage-based charge risk — this screen only needs to plot pins on a street map, not
 * the rest of the Google Maps platform.
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

  // osmdroid's tile cache and the User-Agent it sends to the OSM tile servers both need setting up
  // once, before the first MapView is created. The OSM usage policy requires a real User-Agent —
  // sending the default one gets an app's requests blocked.
  LaunchedEffect(Unit) {
    val prefs = context.getSharedPreferences("osmdroid", 0)
    Configuration.getInstance().load(context, prefs)
    Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
  }

  val gridState = rememberLazyGridState()
  var mapExpanded by remember { mutableStateOf(true) }
  val mapWeight by
      animateFloatAsState(targetValue = if (mapExpanded) 0.48f else 0.22f, label = "mapWeight")

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }

  LaunchedEffect(reports.isEmpty()) {
    if (reports.isEmpty()) viewModel.resolveFallbackCentre()
  }

  // Frame everything as soon as the set of plotted points changes, so a new capture never lands
  // off-screen.
  LaunchedEffect(reports.map { it.id }, mapViewRef) {
    val map = mapViewRef ?: return@LaunchedEffect
    when {
      reports.size >= 2 -> {
        val box =
            BoundingBox.fromGeoPoints(reports.map { GeoPoint(it.latitude, it.longitude) })
                .increaseByScale(1.3f)
        map.zoomToBoundingBox(box, true, 96)
      }
      reports.size == 1 -> {
        val only = reports.first()
        map.controller.animateTo(GeoPoint(only.latitude, only.longitude), SINGLE_REPORT_ZOOM, 600L)
      }
    }
  }

  LaunchedEffect(uiState.fallbackLatitude, uiState.fallbackLongitude, mapViewRef) {
    val map = mapViewRef ?: return@LaunchedEffect
    val lat = uiState.fallbackLatitude
    val lng = uiState.fallbackLongitude
    if (reports.isEmpty() && lat != null && lng != null) {
      map.controller.animateTo(GeoPoint(lat, lng), CITY_ZOOM, 600L)
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

  // Card tapped -> fly the camera to its pin.
  LaunchedEffect(uiState.selectedReportId, mapViewRef) {
    val map = mapViewRef ?: return@LaunchedEffect
    reports.firstOrNull { it.id == uiState.selectedReportId }?.let {
      map.controller.animateTo(GeoPoint(it.latitude, it.longitude), SINGLE_REPORT_ZOOM, 500L)
    }
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Box(modifier = Modifier.fillMaxWidth().weight(mapWeight)) {
      OsmMap(
          reports = reports,
          selectedReportId = uiState.selectedReportId,
          onMapReady = { mapViewRef = it },
          onMarkerClick = viewModel::select,
          onMapTap = { viewModel.select(null) },
          modifier = Modifier.fillMaxSize(),
      )

      Column(
          modifier = Modifier.align(Alignment.BottomEnd).padding(UrbanSenseTheme.spacing.md),
          verticalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.sm),
      ) {
        if (reports.isNotEmpty()) {
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
}

/**
 * Thin Compose wrapper around osmdroid's `MapView`, which is a plain Android `View` with no Compose
 * API of its own. Markers are rebuilt on every report-list change rather than diffed — the list is
 * at most a few dozen occurrences, so the cost is negligible next to the clarity of not hand-rolling
 * a marker reconciler.
 */
@Composable
private fun OsmMap(
    reports: List<LocalReport>,
    selectedReportId: String?,
    onMapReady: (MapView) -> Unit,
    onMarkerClick: (String) -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
  val currentOnMapTap by rememberUpdatedState(onMapTap)

  AndroidView(
      modifier = modifier,
      factory = { context ->
        MapView(context).apply {
          setTileSource(TileSourceFactory.MAPNIK)
          setMultiTouchControls(true)
          zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
          minZoomLevel = 3.0
          controller.setZoom(CITY_ZOOM)
          controller.setCenter(DEFAULT_CENTRE)

          // A tap that lands on the map itself (not a marker) clears the selection.
          overlays.add(
              MapEventsOverlay(
                  object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                      currentOnMapTap()
                      return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                  }
              )
          )

          onMapReady(this)
        }
      },
      update = { mapView ->
        mapView.overlays.removeAll { it is Marker }
        reports.forEach { report ->
          val isSelected = report.id == selectedReportId
          val marker =
              Marker(mapView).apply {
                position = GeoPoint(report.latitude, report.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinDrawable(mapView.context, colorFor(report.status), isSelected)
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                  currentOnMarkerClick(report.id)
                  true
                }
              }
          // Selected pin drawn last so it renders above any pin it overlaps.
          if (isSelected) mapView.overlays.add(marker) else mapView.overlays.add(0, marker)
        }
        mapView.invalidate()
      },
  )
}

/**
 * Draws a filled circular pin, sized up when [selected] — cheaper than shipping a bitmap asset per
 * status colour and per selection state.
 */
private fun pinDrawable(context: Context, color: Color, selected: Boolean): Drawable {
  val dp = context.resources.displayMetrics.density
  val diameterPx = ((if (selected) 30 else 22) * dp).toInt()
  val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val strokeWidth = diameterPx * 0.14f
  val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
  val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
      }
  val radius = diameterPx / 2f - strokeWidth / 2
  canvas.drawCircle(diameterPx / 2f, diameterPx / 2f, radius, fillPaint)
  canvas.drawCircle(diameterPx / 2f, diameterPx / 2f, radius, strokePaint)
  return BitmapDrawable(context.resources, bitmap)
}

private fun colorFor(rawStatus: String?): Color =
    when (runCatching { ReportStatus.valueOf(rawStatus.orEmpty()) }.getOrNull()) {
      ReportStatus.PROCESSED -> Brand.GreenDeep
      ReportStatus.QUEUED -> Brand.GreenGrass
      ReportStatus.FAILED -> Color(0xFF9B1C1C)
      else -> Color(0xFF8A5A00)
    }
