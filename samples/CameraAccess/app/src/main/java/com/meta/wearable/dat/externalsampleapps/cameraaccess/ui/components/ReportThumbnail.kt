/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.max

/**
 * The photo tile on an occurrence card.
 *
 * Sizing is entirely the caller's: pass `Modifier.size(72.dp)` for a list row, or
 * `Modifier.fillMaxWidth().aspectRatio(4f / 3f)` for the map's grid card.
 */
@Composable
fun ReportThumbnail(
    localImagePath: String?,
    remoteUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
  var bitmap by remember(localImagePath, remoteUrl) { mutableStateOf<Bitmap?>(null) }
  var hasError by remember(localImagePath, remoteUrl) { mutableStateOf(false) }

  // A capture is up to 5 MB of JPEG; decoded at full resolution that is tens of megabytes of ARGB
  // in memory, for a tile a couple of hundred pixels wide. Decoding at a sample size scaled to the
  // widest tile we draw keeps a long history list from thrashing the heap on the low-end phones
  // RNF05 targets.
  val targetPx = with(LocalDensity.current) { 320.dp.roundToPx() }

  LaunchedEffect(localImagePath, remoteUrl, targetPx) {
    withContext(Dispatchers.IO) {
      val decoded =
          runCatching {
                val local = localImagePath?.takeIf { it.isNotEmpty() }?.let(::File)
                when {
                  local != null && local.exists() -> decodeSampled(local.readBytes(), targetPx)
                  !remoteUrl.isNullOrEmpty() ->
                      decodeSampled(URL(remoteUrl).openStream().use { it.readBytes() }, targetPx)
                  else -> null
                }
              }
              .getOrNull()

      bitmap = decoded
      // Previously an unreadable file left the tile stuck on the "waiting" camera icon forever,
      // which reads as "still uploading" rather than "this image is gone".
      hasError = decoded == null
    }
  }

  Box(
      modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
  ) {
    val current = bitmap
    when {
      current != null ->
          Image(
              bitmap = current.asImageBitmap(),
              contentDescription = stringResource(R.string.captured_photo),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
          )
      hasError ->
          Icon(
              imageVector = Icons.Default.BrokenImage,
              contentDescription = stringResource(R.string.thumbnail_unavailable),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(28.dp),
          )
      else ->
          Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = stringResource(R.string.thumbnail_loading),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(28.dp),
          )
    }
  }
}

/** Decodes [bytes] downsampled to roughly [targetPx] on its longest edge. */
private fun decodeSampled(bytes: ByteArray, targetPx: Int): Bitmap? {
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

  var sampleSize = 1
  var longestEdge = max(bounds.outWidth, bounds.outHeight)
  while (longestEdge / 2 >= targetPx) {
    sampleSize *= 2
    longestEdge /= 2
  }

  val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
  return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
