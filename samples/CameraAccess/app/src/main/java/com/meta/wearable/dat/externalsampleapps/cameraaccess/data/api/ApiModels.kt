/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api

import com.google.gson.annotations.SerializedName

/**
 * Data response for POST /reports (201 Created)
 */
data class DetectionBox(
    @SerializedName("label") val label: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("box") val box: List<Float> = emptyList()
)

data class ReportSubmissionResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: ReportData? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("audio_feedback") val audioFeedback: String? = null,
    @SerializedName("detection_result") val detectionResult: String? = null,
    @SerializedName("detections") val detections: List<DetectionBox> = emptyList()
)

data class ReportData(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("audio_feedback") val audioFeedback: String? = null,
    @SerializedName("detection_result") val detectionResult: String? = null,
    @SerializedName("detections") val detections: List<DetectionBox> = emptyList()
)

/**
 * Error response payload for 4xx/5xx status codes
 */
data class ApiErrorResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("audio_feedback") val audioFeedback: String? = null,
    @SerializedName("errors") val errors: List<ApiFieldError>? = null
)

data class ApiFieldError(
    @SerializedName("field") val field: String,
    @SerializedName("message") val message: String
)

/**
 * Data response for GET /reports (History)
 */
data class ReportHistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("data") val data: List<ReportItem> = emptyList()
)

data class ReportItem(
    @SerializedName("id") val id: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double? = null,
    @SerializedName("trigger_type") val triggerType: String,
    @SerializedName("status") val status: String,
    @SerializedName("detection_result") val detectionResult: String? = null,
    @SerializedName("created_at") val createdAt: String
)

enum class TriggerType {
    AUTOMATIC,
    MANUAL
}

enum class ReportStatus {
    PENDING,
    QUEUED,
    PROCESSED,
    FAILED,
    LOCAL_SAVED
}
