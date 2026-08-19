/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R

/**
 * The five bottom-bar destinations.
 *
 * Labels come from `strings.xml` rather than being hardcoded here, and they are kept to one short
 * word: five items is the maximum a Material 3 `NavigationBar` fits, and anything longer than
 * "Histórico" truncates on a narrow phone. That is why the camera tab reads "Óculos" instead of
 * "Óculos Ray-Ban", and settings reads "Ajustes" instead of "Configurações".
 */
enum class AppDestination(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
  DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard),
  MAP("map", R.string.map_nav_label, Icons.Default.Map),
  CAMERA("camera", R.string.nav_camera, Icons.Default.Videocam),
  HISTORY("history", R.string.nav_history, Icons.Default.History),
  SETTINGS("settings", R.string.nav_settings, Icons.Default.Settings);

  companion object {
    val START: AppDestination = DASHBOARD

    fun fromRoute(route: String?): AppDestination =
        entries.firstOrNull { it.route == route } ?: START
  }
}
