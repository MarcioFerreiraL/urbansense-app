/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.annotation.StringRes
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Display labels live in `strings.xml` rather than in the enum: baking Portuguese into a service
 * layer made the state untranslatable and forced the UI to depend on this module just to render a
 * word.
 */
enum class MotionActivityType(@StringRes val labelRes: Int, val isMovement: Boolean) {
    STILL(R.string.motion_still, false),
    WALKING(R.string.motion_walking, true),
    RUNNING(R.string.motion_running, true),
    ON_BICYCLE(R.string.motion_bicycle, true),
    IN_VEHICLE(R.string.motion_vehicle, true),
    UNKNOWN(R.string.motion_unknown, false)
}

data class MotionStatus(
    val activityType: MotionActivityType = MotionActivityType.STILL,
    val confidence: Int = 100,
    val isMoving: Boolean = false,
    val lastStateChangeTimestamp: Long = System.currentTimeMillis()
)

class MotionDetectionManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "UrbanSense:Motion"
        private const val ACCEL_THRESHOLD_WALKING = 12.0 // m/s^2 magnitude delta
        private const val ACCEL_THRESHOLD_VEHICLE = 15.0
        private const val STILL_TIMEOUT_MS = 5000L
    }

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepDetector: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _motionStatus = MutableStateFlow(MotionStatus())
    val motionStatus: StateFlow<MotionStatus> = _motionStatus.asStateFlow()

    private var isListening = false
    private var lastMotionTimestamp = 0L
    private val scope = CoroutineScope(Dispatchers.Default)
    private var stillCheckJob: Job? = null

    fun start() {
        if (isListening) return
        isListening = true

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        startStillTimer()
        Log.i(TAG, "Motion Detection Service started")
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
        stillCheckJob?.cancel()
        _motionStatus.value = MotionStatus(
            activityType = MotionActivityType.STILL,
            confidence = 100,
            isMoving = false
        )
        Log.i(TAG, "Motion Detection Service stopped")
    }

    private fun startStillTimer() {
        stillCheckJob?.cancel()
        stillCheckJob = scope.launch {
            while (isListening) {
                delay(2000L)
                val now = System.currentTimeMillis()
                if (now - lastMotionTimestamp > STILL_TIMEOUT_MS && _motionStatus.value.isMoving) {
                    _motionStatus.value = MotionStatus(
                        activityType = MotionActivityType.STILL,
                        confidence = 90,
                        isMoving = false,
                        lastStateChangeTimestamp = now
                    )
                    Log.d(TAG, "User is now STILL (Sleep Mode activated)")
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                lastMotionTimestamp = now
                if (_motionStatus.value.activityType != MotionActivityType.WALKING) {
                    _motionStatus.value = MotionStatus(
                        activityType = MotionActivityType.WALKING,
                        confidence = 95,
                        isMoving = true,
                        lastStateChangeTimestamp = now
                    )
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())
                val delta = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)

                if (delta > 2.5) {
                    lastMotionTimestamp = now
                    val activity = when {
                        delta > ACCEL_THRESHOLD_VEHICLE -> MotionActivityType.IN_VEHICLE
                        delta > ACCEL_THRESHOLD_WALKING -> MotionActivityType.RUNNING
                        else -> MotionActivityType.WALKING
                    }

                    if (!_motionStatus.value.isMoving || _motionStatus.value.activityType != activity) {
                        _motionStatus.value = MotionStatus(
                            activityType = activity,
                            confidence = 85,
                            isMoving = true,
                            lastStateChangeTimestamp = now
                        )
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Manual motion state override for demo and testing environments
     */
    fun simulateMotionState(activityType: MotionActivityType) {
        lastMotionTimestamp = System.currentTimeMillis()
        _motionStatus.value = MotionStatus(
            activityType = activityType,
            confidence = 100,
            isMoving = activityType.isMovement,
            lastStateChangeTimestamp = System.currentTimeMillis()
        )
    }
}
