/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.api.DetectionBox
import java.io.File
import java.io.FileOutputStream

object ImageAnnotationUtils {

    private const val TAG = "UrbanSense:ImageAnnotation"

    /**
     * Draws bounding boxes and label badges onto a bitmap copy and saves it back to local disk.
     */
    fun annotateAndSaveImage(
        imageFile: File,
        detections: List<DetectionBox>
    ): Boolean {
        if (detections.isEmpty() || !imageFile.exists()) return false

        return try {
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return false
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val imgWidth = mutableBitmap.width.toFloat()
            val imgHeight = mutableBitmap.height.toFloat()

            // Paint for box stroke
            val boxPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = (mutableBitmap.width * 0.008f).coerceAtLeast(6f)
                isAntiAlias = true
            }

            // Paint for label background badge
            val badgePaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Paint for text inside badge
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = (mutableBitmap.width * 0.028f).coerceIn(22f, 44f)
                isAntiAlias = true
                isFakeBoldText = true
            }

            detections.forEach { detection ->
                val box = detection.box
                if (box.size >= 4) {
                    val x1 = box[0]
                    val y1 = box[1]
                    val x2 = box[2]
                    val y2 = box[3]

                    val labelLower = detection.label.lowercase()
                    val isTrash = labelLower.contains("trash") ||
                            labelLower.contains("lixo") ||
                            labelLower.contains("descarte") ||
                            labelLower.contains("residuo")

                    val isPothole = labelLower.contains("pothole") ||
                            labelLower.contains("buraco") ||
                            labelLower.contains("pista") ||
                            labelLower.contains("asfalto")

                    // Vibrant Red (#FF3D00) for Trash, Amber (#FFB300) for Potholes, Emerald Green (#00E676) for generic
                    val strokeColor = when {
                        isTrash -> Color.rgb(255, 61, 0)
                        isPothole -> Color.rgb(255, 179, 0)
                        else -> Color.rgb(0, 230, 118)
                    }

                    boxPaint.color = strokeColor
                    badgePaint.color = strokeColor

                    // Draw bounding box rectangle
                    val rect = RectF(x1, y1, x2, y2)
                    canvas.drawRoundRect(rect, 10f, 10f, boxPaint)

                    // Draw text label badge above top-left of box
                    val confidencePercent = (detection.confidence * 100).toInt()
                    val displayLabel = detection.label.replace("_", " ").uppercase()
                    val labelText = " $displayLabel ${confidencePercent}% "
                    val textWidth = textPaint.measureText(labelText)
                    val textHeight = textPaint.textSize + 12f

                    val badgeY1 = (y1 - textHeight).coerceAtLeast(0f)
                    val badgeY2 = y1.coerceAtMost(imgHeight)

                    val badgeRect = RectF(
                        x1,
                        badgeY1,
                        (x1 + textWidth).coerceAtMost(imgWidth),
                        badgeY2
                    )

                    canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)
                    canvas.drawText(
                        labelText,
                        x1,
                        (y1 - 6f).coerceAtLeast(textPaint.textSize),
                        textPaint
                    )
                }
            }

            // Write modified bitmap back to disk
            FileOutputStream(imageFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            originalBitmap.recycle()
            mutableBitmap.recycle()
            Log.i(TAG, "Successfully annotated image ${imageFile.name} with ${detections.size} bounding box(es).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error annotating image with bounding boxes", e)
            false
        }
    }
}
