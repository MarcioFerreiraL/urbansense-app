/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.camera.CameraViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio.AudioFeedbackManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history.HistoryScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history.HistoryViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.map.MapScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.map.MapViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation.AppDestination
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings.SettingsScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings.SettingsViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAccessScaffold(
    wearablesViewModel: WearablesViewModel,
    dashboardViewModel: DashboardViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    cameraViewModel: CameraViewModel,
    mapViewModel: MapViewModel,
    audioFeedbackManager: AudioFeedbackManager,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    onRequestRecordAudioPermission: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
  val uiState by wearablesViewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Replaces a plain `remember { mutableStateOf(destination) }`, which had no back stack and reset
  // to the dashboard on every rotation.
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = AppDestination.fromRoute(backStackEntry?.destination?.route)

  LaunchedEffect(uiState.recentError?.id) {
    uiState.recentError?.let { error ->
      snackbarHostState.showSnackbar(error.message)
      wearablesViewModel.clearRecentError(error.id)
    }
  }

  UrbanSenseTheme {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
          NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 3.dp,
          ) {
            AppDestination.entries.forEach { destination ->
              val label = stringResource(destination.titleRes)
              NavigationBarItem(
                  selected = currentDestination == destination,
                  onClick = {
                    navController.navigate(destination.route) {
                      // Tabs are peers, not a stack: re-selecting a tab must not pile duplicates on
                      // the back stack, and switching away then back should restore that tab's
                      // scroll position rather than rebuild it.
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                  label = { Text(label, maxLines = 1) },
                  colors =
                      NavigationBarItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          selectedTextColor = MaterialTheme.colorScheme.primary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
              )
            }
          }
        },
        snackbarHost = {
          SnackbarHost(
              hostState = snackbarHostState,
              modifier = Modifier.padding(16.dp),
              snackbar = { data ->
                Snackbar(
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription =
                            stringResource(R.string.scaffold_error_icon_description),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(data.visuals.message)
                  }
                }
              },
          )
        },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.START.route,
            modifier = Modifier.fillMaxSize(),
        ) {
          composable(AppDestination.DASHBOARD.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCamera = { navController.navigate(AppDestination.CAMERA.route) },
            )
          }

          composable(AppDestination.MAP.route) {
            MapScreen(
                viewModel = mapViewModel,
                onNavigateToCapture = { navController.navigate(AppDestination.CAMERA.route) },
            )
          }

          composable(AppDestination.CAMERA.route) {
            if (uiState.isRegistered) {
              CameraScreen(
                  wearablesViewModel = wearablesViewModel,
                  onRequestWearablesPermission = onRequestWearablesPermission,
                  onRequestRecordAudioPermission = onRequestRecordAudioPermission,
                  cameraViewModel = cameraViewModel,
              )
            } else {
              HomeScreen(viewModel = wearablesViewModel)
            }
          }

          composable(AppDestination.HISTORY.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onSpeak = { text -> audioFeedbackManager.speak(text) },
            )
          }

          composable(AppDestination.SETTINGS.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onOpenMockDeviceKit = { wearablesViewModel.showDebugMenu() },
            )
          }
        }

        if (BuildConfig.DEBUG) {
          FloatingActionButton(
              onClick = { wearablesViewModel.showDebugMenu() },
              modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = stringResource(R.string.debug_menu_description),
            )
          }

          if (uiState.isDebugMenuVisible) {
            ModalBottomSheet(
                onDismissRequest = { wearablesViewModel.hideDebugMenu() },
                sheetState = bottomSheetState,
                modifier = Modifier.fillMaxSize(),
            ) {
              MockDeviceKitScreen(modifier = Modifier.fillMaxSize())
            }
          }
        }
      }
    }
  }
}
