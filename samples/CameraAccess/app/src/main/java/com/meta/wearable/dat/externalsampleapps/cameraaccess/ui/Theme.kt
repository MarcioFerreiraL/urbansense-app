/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColor {
  val Green = Color(0xFF10B981)
  val Emerald = Color(0xFF059669)
  val EmeraldGlow = Color(0xFF34D399)
  val Red = Color(0xFFEF4444)
  val Yellow = Color(0xFFF59E0B)
  val Amber = Color(0xFFD97706)
  val DeepBlue = Color(0xFF059669)
  val CyanAccent = Color(0xFF10B981)
  val DarkSurface = Color(0xFFFFFFFF)
  val DarkSurfaceCard = Color(0xFFFFFFFF)
  val DarkSurfaceElevated = Color(0xFFF8FAFC)
  val DestructiveBackground = Color(0xFFFFD8DB)
  val DestructiveForeground = Color(0xFFAA071E)
  val RecordAccent = Color(0xFFFF453A)
  val UpdateRequiredBackground = Color(0xFFFEF2F2)
  val UpdateRequiredForeground = Color(0xFF991B1B)
}

// Clean Bright White Theme (White background with subtle green details)
private val CleanWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF059669),            // Emerald Green for buttons & icons
    onPrimary = Color.White,                 // White text on green buttons
    primaryContainer = Color(0xFFDCFCE7),    // Light Mint Container Accent
    onPrimaryContainer = Color(0xFF065F46),  // Dark Emerald Text
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF047857),
    background = Color(0xFFF8FAFC),          // Crisp White/Off-White Background
    onBackground = Color(0xFF0F172A),        // Dark Slate Text
    surface = Color(0xFFFFFFFF),             // Pure White Cards
    onSurface = Color(0xFF0F172A),           // Dark Slate Text on Cards
    surfaceVariant = Color(0xFFFFFFFF),      // Pure White Cards Fill
    onSurfaceVariant = Color(0xFF475569),    // Slate Subtitles
    outline = Color(0xFFE2E8F0)              // Light Subtle Border
)

@Composable
fun UrbanSenseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanWhiteColorScheme,
        content = content
    )
}
