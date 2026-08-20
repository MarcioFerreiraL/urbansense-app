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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
fun FullscreenPhotoViewerDialog(
    localImagePath: String?,
    remoteUrl: String?,
    title: String? = "Foto da Ocorrência",
    onDismissRequest: () -> Unit
) {
    var bitmap by remember(localImagePath, remoteUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(localImagePath, remoteUrl) { mutableStateOf(true) }

    LaunchedEffect(localImagePath, remoteUrl) {
        withContext(Dispatchers.IO) {
            val decoded = runCatching {
                val local = localImagePath?.takeIf { it.isNotEmpty() }?.let(::File)
                when {
                    local != null && local.exists() -> BitmapFactory.decodeFile(local.absolutePath)
                    !remoteUrl.isNullOrEmpty() -> URL(remoteUrl).openStream().use { BitmapFactory.decodeStream(it) }
                    else -> null
                }
            }.getOrNull()
            bitmap = decoded
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val currentBitmap = bitmap
            when {
                currentBitmap != null -> {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Foto da Ocorrência em Alta Resolução",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onDismissRequest() },
                        contentScale = ContentScale.Fit
                    )
                }
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                else -> {
                    Text(
                        text = "Foto não disponível",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0x99000000))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title ?: "Foto da Ocorrência",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .background(Color(0x33FFFFFF), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
