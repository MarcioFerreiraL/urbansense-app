/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Painel", Icons.Default.Dashboard),
    CAMERA("camera", "Óculos Ray-Ban", Icons.Default.Videocam),
    HISTORY("history", "Histórico", Icons.Default.History),
    SETTINGS("settings", "Configurações", Icons.Default.Settings)
}
