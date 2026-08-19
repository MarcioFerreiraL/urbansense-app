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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
fun ReportThumbnail(
    localImagePath: String?,
    remoteUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    cornerRadius: Dp = 12.dp
) {
    var bitmap by remember(localImagePath, remoteUrl) { mutableStateOf<Bitmap?>(null) }
    var hasError by remember(localImagePath, remoteUrl) { mutableStateOf(false) }

    LaunchedEffect(localImagePath, remoteUrl) {
        withContext(Dispatchers.IO) {
            try {
                if (!localImagePath.isNullOrEmpty()) {
                    val file = File(localImagePath)
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        return@withContext
                    }
                }
                if (!remoteUrl.isNullOrEmpty()) {
                    val stream = URL(remoteUrl).openStream()
                    bitmap = BitmapFactory.decodeStream(stream)
                    return@withContext
                }
            } catch (e: Exception) {
                hasError = true
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Foto capturada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            hasError -> {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Imagem indisponível",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Aguardando imagem",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
