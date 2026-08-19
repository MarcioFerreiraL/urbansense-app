/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.service.motion.MotionActivityType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportThumbnail
import java.io.File


@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionStatus by viewModel.motionStatus.collectAsState()
    val captureState by viewModel.captureState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val latestReport by viewModel.latestReport.collectAsState()
    val totalReports by viewModel.totalReportsCount.collectAsState()
    val wearablesState by viewModel.wearablesViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UrbanSense AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Thin Client & Movimento Inteligente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Connection Chip
            val isConnected = wearablesState.hasActiveDevice
            val chipBg = if (isConnected) Color(0xFF065F46) else Color(0xFF374151)
            val chipText = if (isConnected) "Ray-Ban Conectado" else "Sem Óculos"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = chipBg,
                modifier = Modifier.clickable { onNavigateToCamera() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF34D399) else Color(0xFF9CA3AF))
                    )
                    Text(
                        text = chipText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Motion & Power Efficiency Card (RF02)
        val isMoving = motionStatus.isMoving
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isMoving) 1.08f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isMoving) AppColor.Emerald.copy(alpha = 0.2f)
                                    else AppColor.Yellow.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (motionStatus.activityType) {
                                    MotionActivityType.WALKING -> Icons.Default.DirectionsWalk
                                    MotionActivityType.RUNNING -> Icons.Default.DirectionsRun
                                    MotionActivityType.ON_BICYCLE -> Icons.Default.DirectionsBike
                                    MotionActivityType.IN_VEHICLE -> Icons.Default.Bolt
                                    else -> Icons.Default.NightlightRound
                                },
                                contentDescription = "Motion Status",
                                tint = if (isMoving) AppColor.Emerald else AppColor.Yellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Detecção de Movimento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = motionStatus.activityType.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMoving) AppColor.Emerald else Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sleep / Active Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (captureState.isSleepMode) Color(0xFF1E293B) else Color(0xFF064E3B)
                    ) {
                        Text(
                            text = if (captureState.isSleepMode) "Sleep Mode" else "Ativo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (captureState.isSleepMode) Color(0xFF94A3B8) else Color(0xFFA7F3D0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Energy details description
                Text(
                    text = if (captureState.isSleepMode) {
                        "Modo de economia ativa: Câmera e GPS sob demanda em repouso."
                    } else {
                        "Usuário em movimento ativo: Captura periódica automática habilitada."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulation controls (for testing without walking outdoors)
                Text(
                    text = "Simular Estado de Movimento (Teste):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.simulateMotion(MotionActivityType.STILL) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("Parado", fontSize = 11.sp, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.simulateMotion(MotionActivityType.WALKING) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("Caminhando", fontSize = 11.sp, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.simulateMotion(MotionActivityType.RUNNING) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("Correndo", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Auto-Capture & Interval Status Card (RF03)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Captura Automática Periódica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Intervalo: ${settings.autoCaptureIntervalSec}s em movimento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = captureState.isAutoCaptureActive,
                        onCheckedChange = { viewModel.toggleAutoCapture() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColor.Emerald,
                            checkedTrackColor = AppColor.Emerald.copy(alpha = 0.4f)
                        )
                    )
                }

                if (captureState.countdownSec > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { captureState.countdownSec.toFloat() / settings.autoCaptureIntervalSec },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppColor.Emerald
                        )
                        Text(
                            text = "Próximo disparo automático em ${captureState.countdownSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColor.Emerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Large CTA Manual Capture Button (RF03)
        val isCapturing = captureState.isCapturingNow
        Button(
            onClick = { viewModel.triggerManualCapture() },
            enabled = !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColor.Emerald,
                contentColor = Color.White
            )
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Obtendo GPS & Enviando...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Disparo Manual",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Registrar Ocorrência Manual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Latest Report Card (RF05 & RF06)
        if (latestReport != null) {
            Text(
                text = "Último Registro Enviado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LatestReportItemCard(
                report = latestReport!!,
                onTestAudio = { viewModel.testAudioFeedback() },
                onOpenMap = { lat, lng ->
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Ocorrência+UrbanSense)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(mapIntent)
                }
            )
        }

        // Session Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Total de Envios",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalReports",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Disparos na Sessão",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${captureState.totalCapturesInSession}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun LatestReportItemCard(
    report: LocalReport,
    onTestAudio: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail or Placeholder
                ReportThumbnail(
                    localImagePath = report.localImagePath,
                    remoteUrl = report.remoteThumbnailUrl,
                    size = 80.dp,
                    cornerRadius = 12.dp
                )


                Spacer(modifier = Modifier.width(12.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (report.status) {
                                "QUEUED", "PROCESSED" -> Color(0xFF065F46)
                                "FAILED" -> Color(0xFF7F1D1D)
                                else -> Color(0xFF1E3A8A)
                            }
                        ) {
                            Text(
                                text = report.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = report.triggerType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "GPS: %.5f, %.5f".format(report.latitude, report.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = report.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!report.audioFeedback.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio feedback",
                                tint = AppColor.Emerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = report.audioFeedback,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColor.Emerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenMap(report.latitude, report.longitude) },
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ver no mapa",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver no Mapa", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onTestAudio,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ouvir feedback",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
