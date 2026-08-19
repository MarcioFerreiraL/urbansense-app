/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db.LocalReport
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components.ReportThumbnail

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reports.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Histórico de Envios",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${reports.size} ocorrências registradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.refreshFromApi() },
                    enabled = !uiState.isSyncing
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar com API",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (reports.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpar Histórico",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (uiState.syncErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.syncErrorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Nenhuma ocorrência registrada ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Faça uma captura manual ou inicie a detecção para registrar eventos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportHistoryCard(
                        report = report,
                        onClick = { viewModel.selectReport(report) },
                        onOpenMap = { lat, lng ->
                            openGoogleMaps(context, lat, lng)
                        },
                        onPlayAudio = {
                            val msg = report.audioFeedback ?: "Ocorrência ${getStatusLabel(report.status)}"
                            onSpeak(msg)
                        },
                        onRetry = {
                            viewModel.retryReport(report.id)
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    uiState.selectedReport?.let { report ->
        ReportDetailDialog(
            report = report,
            isSyncing = uiState.isSyncing,
            onDismiss = { viewModel.selectReport(null) },
            onOpenMap = { lat, lng -> openGoogleMaps(context, lat, lng) },
            onPlayAudio = {
                val msg = report.audioFeedback ?: "Status: ${getStatusLabel(report.status)}"
                onSpeak(msg)
            },
            onRetry = {
                viewModel.retryReport(report.id)
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar Histórico") },
            text = { Text("Tem certeza que deseja apagar todos os registros salvos localmente?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ReportHistoryCard(
    report: LocalReport,
    onClick: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onPlayAudio: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail
                ReportThumbnail(
                    localImagePath = report.localImagePath,
                    remoteUrl = report.remoteThumbnailUrl,
                    size = 72.dp,
                    cornerRadius = 10.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = report.status)

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = if (report.triggerType == "MANUAL") "MANUAL" else "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                        Text(
                            text = report.audioFeedback,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (report.status == "PENDING") Color(0xFFD97706) else AppColor.Emerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (report.status == "PENDING" || report.status == "FAILED" || report.status == "LOCAL_SAVED") {
                    OutlinedButton(
                        onClick = onRetry,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Reenviar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reenviar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = { onOpenMap(report.latitude, report.longitude) },
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Mapa",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver no Mapa", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Reproduzir áudio",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: LocalReport,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onPlayAudio: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            if (report.status == "PENDING" || report.status == "FAILED" || report.status == "LOCAL_SAVED") {
                Button(
                    onClick = onRetry,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reenviar para API")
                    }
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalhes da Ocorrência",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = report.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    ReportThumbnail(
                        localImagePath = report.localImagePath,
                        remoteUrl = report.remoteThumbnailUrl,
                        size = 200.dp,
                        cornerRadius = 12.dp
                    )
                }

                HorizontalDivider()

                // Info Rows
                DetailRow(label = "ID do Registro", value = report.id)
                DetailRow(label = "Origem do Disparo", value = if (report.triggerType == "MANUAL") "Manual (Botão / Haste)" else "Automático (Movimento)")
                DetailRow(label = "Data e Hora", value = report.timestamp)
                DetailRow(
                    label = "Coordenadas GPS",
                    value = "Lat: %.6f\nLng: %.6f\nPrecisão: %.1fm".format(report.latitude, report.longitude, report.accuracy)
                )

                if (!report.audioFeedback.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Feedback da IA / Resposta:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.audioFeedback,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (!report.errorMessage.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Detalhes do Erro de Conexão:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenMap(report.latitude, report.longitude) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver no Mapa")
                    }

                    OutlinedButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ouvir Áudio")
                    }
                }
            }
        }
    )
}

@Composable
fun StatusChip(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "PENDENTE")
        "QUEUED", "PROCESSED" -> Triple(Color(0xD16FDF6), Color(0xFF065F46), "ENVIADO")
        "FAILED" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "FALHA")
        else -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "SALVO LOCAL")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun openGoogleMaps(context: android.content.Context, lat: Double, lng: Double) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Ocorrência+UrbanSense)")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(mapIntent)
}

private fun getStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendente de Envio"
    "QUEUED", "PROCESSED" -> "Enviado com sucesso"
    "FAILED" -> "Falha no envio"
    else -> "Salvo localmente"
}
