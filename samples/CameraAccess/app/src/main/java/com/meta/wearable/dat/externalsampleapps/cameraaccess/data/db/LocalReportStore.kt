/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class LocalReport(
    val id: String,
    val localImagePath: String? = null,
    val remoteThumbnailUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val timestamp: String,
    val triggerType: String,
    val status: String,
    val detectionResult: String? = null,
    val audioFeedback: String? = null,
    val errorMessage: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

class LocalReportStore(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val _reportsFlow = MutableStateFlow<List<LocalReport>>(emptyList())
    val reportsFlow: Flow<List<LocalReport>> = _reportsFlow.asStateFlow()

    init {
        refreshReports()
    }

    private fun refreshReports() {
        val list = getAllReportsInternal()
        _reportsFlow.value = list
    }

    suspend fun insertOrUpdate(report: LocalReport): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_ID, report.id)
            put(DatabaseHelper.COLUMN_LOCAL_IMAGE, report.localImagePath)
            put(DatabaseHelper.COLUMN_REMOTE_THUMBNAIL, report.remoteThumbnailUrl)
            put(DatabaseHelper.COLUMN_LATITUDE, report.latitude)
            put(DatabaseHelper.COLUMN_LONGITUDE, report.longitude)
            put(DatabaseHelper.COLUMN_ACCURACY, report.accuracy)
            put(DatabaseHelper.COLUMN_TIMESTAMP, report.timestamp)
            put(DatabaseHelper.COLUMN_TRIGGER_TYPE, report.triggerType)
            put(DatabaseHelper.COLUMN_STATUS, report.status)
            put(DatabaseHelper.COLUMN_DETECTION_RESULT, report.detectionResult)
            put(DatabaseHelper.COLUMN_AUDIO_FEEDBACK, report.audioFeedback)
            put(DatabaseHelper.COLUMN_ERROR_MESSAGE, report.errorMessage)
            put(DatabaseHelper.COLUMN_CREATED_AT_MILLIS, report.createdAtMillis)
        }
        val rowId = db.insertWithOnConflict(
            DatabaseHelper.TABLE_REPORTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshReports()
        rowId
    }

    suspend fun getAllReports(): List<LocalReport> = withContext(Dispatchers.IO) {
        getAllReportsInternal()
    }

    suspend fun getReportById(id: String): LocalReport? = withContext(Dispatchers.IO) {
        getAllReportsInternal().find { it.id == id }
    }

    suspend fun deleteReport(id: String): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete(DatabaseHelper.TABLE_REPORTS, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(id))
        refreshReports()
        deleted
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete(DatabaseHelper.TABLE_REPORTS, null, null)
        refreshReports()
        deleted
    }

    private fun getAllReportsInternal(): List<LocalReport> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_REPORTS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_CREATED_AT_MILLIS} DESC"
        )
        val reports = mutableListOf<LocalReport>()
        cursor.use {
            while (it.moveToNext()) {
                reports.add(
                    LocalReport(
                        id = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        localImagePath = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LOCAL_IMAGE)),
                        remoteThumbnailUrl = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMOTE_THUMBNAIL)),
                        latitude = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE)),
                        longitude = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE)),
                        accuracy = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACCURACY))) null else it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACCURACY)),
                        timestamp = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)),
                        triggerType = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRIGGER_TYPE)),
                        status = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)),
                        detectionResult = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETECTION_RESULT)),
                        audioFeedback = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AUDIO_FEEDBACK)),
                        errorMessage = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ERROR_MESSAGE)),
                        createdAtMillis = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT_MILLIS))
                    )
                )
            }
        }
        return reports
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
        companion object {
            private const val DATABASE_NAME = "urbansense_reports.db"
            private const val DATABASE_VERSION = 1

            const val TABLE_REPORTS = "reports"
            const val COLUMN_ID = "id"
            const val COLUMN_LOCAL_IMAGE = "local_image_path"
            const val COLUMN_REMOTE_THUMBNAIL = "remote_thumbnail_url"
            const val COLUMN_LATITUDE = "latitude"
            const val COLUMN_LONGITUDE = "longitude"
            const val COLUMN_ACCURACY = "accuracy"
            const val COLUMN_TIMESTAMP = "timestamp"
            const val COLUMN_TRIGGER_TYPE = "trigger_type"
            const val COLUMN_STATUS = "status"
            const val COLUMN_DETECTION_RESULT = "detection_result"
            const val COLUMN_AUDIO_FEEDBACK = "audio_feedback"
            const val COLUMN_ERROR_MESSAGE = "error_message"
            const val COLUMN_CREATED_AT_MILLIS = "created_at_millis"
        }

        override fun onCreate(db: SQLiteDatabase) {
            val createSql = """
                CREATE TABLE $TABLE_REPORTS (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_LOCAL_IMAGE TEXT,
                    $COLUMN_REMOTE_THUMBNAIL TEXT,
                    $COLUMN_LATITUDE REAL NOT NULL,
                    $COLUMN_LONGITUDE REAL NOT NULL,
                    $COLUMN_ACCURACY REAL,
                    $COLUMN_TIMESTAMP TEXT NOT NULL,
                    $COLUMN_TRIGGER_TYPE TEXT NOT NULL,
                    $COLUMN_STATUS TEXT NOT NULL,
                    $COLUMN_DETECTION_RESULT TEXT,
                    $COLUMN_AUDIO_FEEDBACK TEXT,
                    $COLUMN_ERROR_MESSAGE TEXT,
                    $COLUMN_CREATED_AT_MILLIS INTEGER NOT NULL
                )
            """.trimIndent()
            db.execSQL(createSql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
            onCreate(db)
        }
    }
}
