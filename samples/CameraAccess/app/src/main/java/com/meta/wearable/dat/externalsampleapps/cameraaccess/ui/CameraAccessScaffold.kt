/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation.AppDestination
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings.SettingsScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings.SettingsViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAccessScaffold(
    wearablesViewModel: WearablesViewModel,
    dashboardViewModel: DashboardViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    cameraViewModel: CameraViewModel,
    audioFeedbackManager: AudioFeedbackManager,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    onRequestRecordAudioPermission: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by wearablesViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    // Observe recent errors and show snackbar
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
                    tonalElevation = 8.dp
                ) {
                    AppDestination.entries.forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    snackbar = { data ->
                        Snackbar(
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = stringResource(R.string.scaffold_error_icon_description),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(data.visuals.message)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToCamera = { currentDestination = AppDestination.CAMERA }
                        )
                    }
                    AppDestination.CAMERA -> {
                        if (uiState.isRegistered) {
                            CameraScreen(
                                wearablesViewModel = wearablesViewModel,
                                onRequestWearablesPermission = onRequestWearablesPermission,
                                onRequestRecordAudioPermission = onRequestRecordAudioPermission,
                                cameraViewModel = cameraViewModel
                            )
                        } else {
                            HomeScreen(
                                viewModel = wearablesViewModel,
                            )
                        }
                    }
                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onSpeak = { text -> audioFeedbackManager.speak(text) }
                        )
                    }
                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onOpenMockDeviceKit = { wearablesViewModel.showDebugMenu() }
                        )
                    }
                }

                if (BuildConfig.DEBUG) {
                    FloatingActionButton(
                        onClick = { wearablesViewModel.showDebugMenu() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                        containerColor = AppColor.Emerald,
                        contentColor = Color.White,
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
