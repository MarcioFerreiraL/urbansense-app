/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR,
    NETWORK,
    DAT_SDK
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()),
    val fullDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableStackTrace: String? = null
)

object AppLogger {
    private const val MAX_LOG_ENTRIES = 1000
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var logFile: File? = null

    fun init(context: Context) {
        try {
            val logsDir = File(context.filesDir, "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            logFile = File(logsDir, "urbansense_app.log")
            i("UrbanSense:Logger", "Sistema de monitoramento ativado. Arquivo: ${logFile?.name}")
        } catch (e: Exception) {
            Log.e("UrbanSense:Logger", "Erro ao inicializar arquivo de log", e)
        }
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun net(tag: String, message: String) = log(LogLevel.NETWORK, tag, message)
    fun sdk(tag: String, message: String) = log(LogLevel.DAT_SDK, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val now = Date()
        val stackTraceStr = throwable?.let { Log.getStackTraceString(it) }

        // Output to Logcat
        when (level) {
            LogLevel.INFO, LogLevel.DAT_SDK -> Log.i(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.NETWORK -> Log.d(tag, message, throwable)
        }

        val entry = LogEntry(
            timestamp = timeFormat.format(now),
            fullDate = dateFormat.format(now),
            level = level,
            tag = tag,
            message = message,
            throwableStackTrace = stackTraceStr
        )

        scope.launch {
            val currentList = _logsFlow.value.toMutableList()
            currentList.add(0, entry) // Newest entry at index 0
            if (currentList.size > MAX_LOG_ENTRIES) {
                currentList.removeAt(currentList.lastIndex)
            }
            _logsFlow.value = currentList

            // Write entry to local log file
            logFile?.let { file ->
                try {
                    FileWriter(file, true).use { writer ->
                        writer.append("[${entry.fullDate}] [${entry.level}] [${entry.tag}] ${entry.message}\n")
                        if (stackTraceStr != null) {
                            writer.append("$stackTraceStr\n")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UrbanSense:Logger", "Falha ao gravar log no arquivo", e)
                }
            }
        }
    }

    fun getExportableLogFile(context: Context): File {
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) logsDir.mkdirs()
        val exportFile = File(logsDir, "urbansense_logs_${System.currentTimeMillis()}.txt")

        try {
            FileWriter(exportFile).use { writer ->
                writer.append("=========================================\n")
                writer.append("URBANSENSE AI - RELATÓRIO DE LOGS DO SISTEMA\n")
                writer.append("Data de Exportação: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.append("Total de Registros: ${_logsFlow.value.size}\n")
                writer.append("=========================================\n\n")

                _logsFlow.value.reversed().forEach { entry ->
                    writer.append("[${entry.fullDate}] [${entry.level.name}] [${entry.tag}]\n")
                    writer.append("${entry.message}\n")
                    if (!entry.throwableStackTrace.isNullOrBlank()) {
                        writer.append("STACK TRACE:\n${entry.throwableStackTrace}\n")
                    }
                    writer.append("-----------------------------------------\n")
                }
            }
        } catch (e: Exception) {
            Log.e("UrbanSense:Logger", "Erro ao gerar arquivo exportado", e)
        }
        return exportFile
    }

    fun getFormattedLogsText(): String {
        return buildString {
            append("--- URBANSENSE AI SYSTEM LOGS ---\n\n")
            _logsFlow.value.reversed().forEach { entry ->
                append("[${entry.timestamp}] [${entry.level.name}] [${entry.tag}]\n")
                append("${entry.message}\n")
                if (!entry.throwableStackTrace.isNullOrBlank()) {
                    append("${entry.throwableStackTrace}\n")
                }
                append("\n")
            }
        }
    }

    
    fun getFormattedLogString(): String = getFormattedLogsText()
    fun clear() = clearLogs()

    fun exportLogsAndShare(context: Context) {
        val file = getExportableLogFile(context)
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Exportar Logs UrbanSense"))
        } catch (e: Exception) {
            Log.e("UrbanSense:Logger", "Erro ao compartilhar logs", e)
        }
    }

    fun clearLogs() {
        scope.launch {
            _logsFlow.value = emptyList()
            try {
                logFile?.writeText("")
            } catch (e: Exception) {
                Log.e("UrbanSense:Logger", "Erro ao limpar arquivo de log", e)
            }
        }
    }
}
