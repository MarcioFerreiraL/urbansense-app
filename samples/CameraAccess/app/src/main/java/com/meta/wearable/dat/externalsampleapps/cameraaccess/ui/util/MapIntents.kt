/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the device's map application at [latitude]/[longitude].
 *
 * The in-app map covers the common case now; this stays for "abrir no Google Maps", where the user
 * wants turn-by-turn directions to an occurrence. The URI was previously assembled inline in both
 * the dashboard and the history screen, character for character.
 *
 * @return false when no app on the device can handle a `geo:` intent, so the caller can surface a
 *   message instead of the tap silently doing nothing.
 */
fun openExternalMap(context: Context, latitude: Double, longitude: Double, label: String): Boolean {
  val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
  return try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    true
  } catch (e: ActivityNotFoundException) {
    false
  }
}
