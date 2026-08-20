/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun ReportThumbnail(
    localImagePath: String?,
    remoteUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: (() -> Unit)? = null,
) {
  var bitmap by remember(localImagePath, remoteUrl) { mutableStateOf<Bitmap?>(null) }
  var hasError by remember(localImagePath, remoteUrl) { mutableStateOf(false) }

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
      hasError = decoded == null
    }
  }

  val boxModifier = if (onClick != null && bitmap != null) {
    modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onClick() }
  } else {
    modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
  }

  Box(
      modifier = boxModifier,
      contentAlignment = Alignment.Center,
  ) {
    val current = bitmap
    when {
      current != null -> {
          Image(
              bitmap = current.asImageBitmap(),
              contentDescription = stringResource(R.string.captured_photo),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
          )
          if (onClick != null) {
              Box(
                  modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .padding(4.dp)
                      .background(Color(0xAA000000), CircleShape)
                      .padding(4.dp)
              ) {
                  Icon(
                      imageVector = Icons.Default.ZoomIn,
                      contentDescription = "Ver foto inteira",
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                  )
              }
          }
      }
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
