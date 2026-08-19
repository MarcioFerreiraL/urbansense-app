/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.INTERNET
import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.camera.CameraViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReportStore
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.ReportRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.repository.SettingsRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio.AudioFeedbackManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.capture.AutoCaptureEngine
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.location.LocationManagerHelper
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionDetectionManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.CameraAccessScaffold
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history.HistoryViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.settings.SettingsViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

  companion object {
    val PERMISSIONS: Array<String> by lazy {
      buildList {
        add(BLUETOOTH)
        add(BLUETOOTH_CONNECT)
        add(INTERNET)
        add(ACCESS_FINE_LOCATION)
        add(ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          add(ACTIVITY_RECOGNITION)
        }
      }.toTypedArray()
    }
  }

  val wearablesViewModel: WearablesViewModel by viewModels()

  private lateinit var locationManagerHelper: LocationManagerHelper
  private lateinit var motionDetectionManager: MotionDetectionManager
  private lateinit var localReportStore: LocalReportStore
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var audioFeedbackManager: AudioFeedbackManager
  private lateinit var reportRepository: ReportRepository
  private lateinit var autoCaptureEngine: AutoCaptureEngine

  private lateinit var cameraViewModel: CameraViewModel
  private lateinit var dashboardViewModel: DashboardViewModel
  private lateinit var historyViewModel: HistoryViewModel
  private lateinit var settingsViewModel: SettingsViewModel

  private val permissionCheckLauncher =
      registerForActivityResult(RequestMultiplePermissions()) { permissionsResult ->
        wearablesViewModel.onPermissionsResult(permissionsResult) {
          Wearables.initialize(this)
          motionDetectionManager.start()
        }
      }

  private var permissionContinuation: CancellableContinuation<PermissionStatus>? = null
  private val permissionMutex = Mutex()

  private val permissionsResultLauncher =
      registerForActivityResult(Wearables.RequestPermissionContract()) { result ->
        val permissionStatus = result.getOrDefault(PermissionStatus.Denied)
        permissionContinuation?.resume(permissionStatus)
        permissionContinuation = null
      }

  suspend fun requestWearablesPermission(permission: Permission): PermissionStatus {
    return permissionMutex.withLock {
      suspendCancellableCoroutine { continuation ->
        permissionContinuation = continuation
        continuation.invokeOnCancellation { permissionContinuation = null }
        permissionsResultLauncher.launch(permission)
      }
    }
  }

  private var audioPermissionContinuation: CancellableContinuation<Boolean>? = null
  private val recordAudioPermissionLauncher =
      registerForActivityResult(RequestPermission()) { granted ->
        audioPermissionContinuation?.resume(granted)
        audioPermissionContinuation = null
      }

  suspend fun requestRecordAudioPermission(): Boolean {
    if (
        ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) {
      return true
    }
    return permissionMutex.withLock {
      suspendCancellableCoroutine { continuation ->
        audioPermissionContinuation = continuation
        continuation.invokeOnCancellation { audioPermissionContinuation = null }
        recordAudioPermissionLauncher.launch(RECORD_AUDIO)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Meta Wearables DAT SDK first before any ViewModel or Selector accesses it
    Wearables.initialize(applicationContext)

    // Initialize core services & repositories
    locationManagerHelper = LocationManagerHelper(this)
    motionDetectionManager = MotionDetectionManager(this)
    localReportStore = LocalReportStore(this)
    settingsRepository = SettingsRepository(this)
    audioFeedbackManager = AudioFeedbackManager(this)
    reportRepository = ReportRepository(
        context = this,
        reportStore = localReportStore,
        settingsRepository = settingsRepository,
        locationManagerHelper = locationManagerHelper,
        audioFeedbackManager = audioFeedbackManager
    )

    val cameraFactory = CameraViewModel.Factory(application, wearablesViewModel)
    cameraViewModel = cameraFactory.create(CameraViewModel::class.java)

    autoCaptureEngine = AutoCaptureEngine(
        motionDetectionManager = motionDetectionManager,
        reportRepository = reportRepository,
        settingsRepository = settingsRepository,
        capturePhotoProvider = { cameraViewModel.capturePhotoDirectly() }
    )

    // Sync streaming state with auto-capture engine
    lifecycleScope.launch {
      cameraViewModel.uiState.collect { cameraState ->
        autoCaptureEngine.setStreamingActive(cameraState.isStreaming)
      }
    }

    dashboardViewModel = DashboardViewModel(
        application = application,
        motionDetectionManager = motionDetectionManager,
        autoCaptureEngine = autoCaptureEngine,
        reportRepository = reportRepository,
        settingsRepository = settingsRepository,
        audioFeedbackManager = audioFeedbackManager,
        wearablesViewModel = wearablesViewModel
    )

    historyViewModel = HistoryViewModel(
        application = application,
        reportRepository = reportRepository
    )

    settingsViewModel = SettingsViewModel(
        application = application,
        settingsRepository = settingsRepository,
        audioFeedbackManager = audioFeedbackManager
    )

    setContent {
      CameraAccessScaffold(
          wearablesViewModel = wearablesViewModel,
          dashboardViewModel = dashboardViewModel,
          historyViewModel = historyViewModel,
          settingsViewModel = settingsViewModel,
          cameraViewModel = cameraViewModel,
          audioFeedbackManager = audioFeedbackManager,
          onRequestWearablesPermission = ::requestWearablesPermission,
          onRequestRecordAudioPermission = ::requestRecordAudioPermission,
      )
    }
  }

  override fun onStart() {
    super.onStart()
    permissionCheckLauncher.launch(PERMISSIONS)
  }

  override fun onDestroy() {
    super.onDestroy()
    motionDetectionManager.stop()
    audioFeedbackManager.cleanup()
  }
}
