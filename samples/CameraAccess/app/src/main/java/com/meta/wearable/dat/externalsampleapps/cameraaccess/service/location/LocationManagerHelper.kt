/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.service.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

data class TaggedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val timestampIso: String
)

class LocationManagerHelper(private val context: Context) {

    companion object {
        private const val TAG = "UrbanSense:Location"
        private const val GPS_TIMEOUT_MS = 2500L

        fun getIsoUtcTimestamp(date: Date = Date()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(date)
        }
    }

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getOnDemandLocation(): TaggedLocation {
        val nowIso = getIsoUtcTimestamp()
        if (locationManager == null) {
            return fallbackLocation(nowIso)
        }

        // Try fast on-demand location fix with timeout
        val freshLocation = withTimeoutOrNull(GPS_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                cont.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }

                try {
                    when {
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                        }
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                        }
                        else -> {
                            cont.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request single location update", e)
                    cont.resume(null)
                }
            }
        }

        if (freshLocation != null) {
            return TaggedLocation(
                latitude = freshLocation.latitude,
                longitude = freshLocation.longitude,
                accuracy = freshLocation.accuracy.toDouble(),
                timestampIso = nowIso
            )
        }

        // Check best last known location
        val bestLastKnown = getBestLastKnownLocation()
        if (bestLastKnown != null) {
            return TaggedLocation(
                latitude = bestLastKnown.latitude,
                longitude = bestLastKnown.longitude,
                accuracy = bestLastKnown.accuracy.toDouble(),
                timestampIso = nowIso
            )
        }

        return fallbackLocation(nowIso)
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(): Location? {
        if (locationManager == null) return null
        val providers = locationManager.getProviders(true)
        var best: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (best == null || l.accuracy < best.accuracy) {
                best = l
            }
        }
        return best
    }

    private fun fallbackLocation(timestampIso: String): TaggedLocation {
        // Recife coordinates for test / simulated environment
        return TaggedLocation(
            latitude = -8.047562,
            longitude = -34.877014,
            accuracy = 4.5,
            timestampIso = timestampIso
        )
    }
}
