/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.SettingsRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.location.LocationManagerHelper
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util.hasValidLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapUiState(
    /** Report currently highlighted — set by tapping a pin or a card. */
    val selectedReportId: String? = null,
    /** Where the map should sit when there is nothing to frame yet. */
    val fallbackLatitude: Double? = null,
    val fallbackLongitude: Double? = null,
    /** E-mail da prefeitura configurado, mostrado em cada card como "Enviado para". */
    val destinationHost: String = "",
)

class MapViewModel(
    application: Application,
    reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository,
    private val locationManagerHelper: LocationManagerHelper,
) : AndroidViewModel(application) {

  /**
   * Only reports carrying a real fix are plotted. `LocalReport.latitude`/`longitude` are non-null
   * primitives defaulting to 0.0, so an unplottable report would otherwise drop a pin in the Gulf of
   * Guinea and drag the map's bounds across the Atlantic with it.
   */
  val reports: StateFlow<List<LocalReport>> =
      reportRepository.reportsFlow
          .map { list -> list.filter { hasValidLocation(it.latitude, it.longitude) } }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _uiState = MutableStateFlow(MapUiState())
  val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

  init {
    _uiState.value = _uiState.value.copy(destinationHost = settingsRepository.getSettings().cityHallEmail)
  }

  fun select(reportId: String?) {
    _uiState.value = _uiState.value.copy(selectedReportId = reportId)
  }

  /**
   * Resolves a centre for the empty map. Only called when there is nothing to frame, so the GPS
   * radio is not woken up for the common case — RNF03 asks for location on demand only.
   */
  fun resolveFallbackCentre() {
    if (_uiState.value.fallbackLatitude != null) return
    viewModelScope.launch {
      val location = locationManagerHelper.getOnDemandLocation()
      _uiState.value =
          _uiState.value.copy(
              fallbackLatitude = location.latitude,
              fallbackLongitude = location.longitude,
          )
    }
  }

  fun refreshDestination() {
    _uiState.value = _uiState.value.copy(destinationHost = settingsRepository.getSettings().cityHallEmail)
  }
}
