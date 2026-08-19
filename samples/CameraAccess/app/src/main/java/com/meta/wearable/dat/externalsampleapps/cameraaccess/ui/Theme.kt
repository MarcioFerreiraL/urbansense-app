/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

<<<<<<< HEAD
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
=======
import androidx.compose.material3.MaterialTheme
>>>>>>> bbd53f3 (feat: implement UrbanSense AI app architecture with enhanced capture, detection services, and new dashboard navigation.)
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette, derived from the app logo (a green cityscape mark). Kept as a flat object of
// named constants rather than only relying on MaterialTheme.colorScheme because several screens
// (the full-bleed camera viewfinder in particular) intentionally use a fixed dark chrome
// regardless of the system light/dark theme, and reach for these tokens directly.
object AppColor {
<<<<<<< HEAD
  // Core brand greens, sampled from the logo.
  val Primary = Color(0xFF2F8F5C)
  val PrimaryDark = Color(0xFF1F5C3A)
  val PrimaryLight = Color(0xFFDFF3E6)

  // Semantic states. The brand is already green, so Success intentionally shares the primary hue;
  // Info/Warning/Error use distinct hues so they stay legible as separate signals.
  val Success = Primary
  val SuccessBackground = PrimaryLight
  val Info = Color(0xFF0064E0)
  val InfoBackground = Color(0xFFE1EDFF)
  val Warning = Color(0xFF8A4B00)
  val WarningBackground = Color(0xFFFFF4D6)
  val Error = Color(0xFFAA071E)
  val ErrorBackground = Color(0xFFFFD8DB)

  // Existing call sites reference these names directly (status dots, destructive buttons, the
  // record accent, the firmware-update banner) — kept so their meaning now flows from the brand
  // palette above without having to touch every usage.
  val Green = Primary
  val Red = Color(0xFFFF3B30)
  val Yellow = Color(0xFFFFCC00)
  val DeepBlue = Info
  val DestructiveBackground = ErrorBackground
  val DestructiveForeground = Error
  // Recording is a universal red regardless of brand color, so this stays independent of Primary.
  val RecordAccent = Color(0xFFFF453A)
  val UpdateRequiredBackground = WarningBackground
  val UpdateRequiredForeground = Warning
}

private val LightColors =
    lightColorScheme(
        primary = AppColor.Primary,
        onPrimary = Color.White,
        primaryContainer = AppColor.PrimaryLight,
        onPrimaryContainer = AppColor.PrimaryDark,
        secondary = AppColor.PrimaryDark,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3EFE7),
        onSecondaryContainer = AppColor.PrimaryDark,
        tertiary = AppColor.Info,
        onTertiary = Color.White,
        tertiaryContainer = AppColor.InfoBackground,
        onTertiaryContainer = Color(0xFF00376B),
        background = Color(0xFFF7FAF8),
        onBackground = Color(0xFF16241D),
        surface = Color.White,
        onSurface = Color(0xFF16241D),
        surfaceVariant = Color(0xFFEAF0EC),
        onSurfaceVariant = Color(0xFF4B5A52),
        outline = Color(0xFFC6D2CB),
        outlineVariant = Color(0xFFDDE5E0),
        error = AppColor.Error,
        onError = Color.White,
        errorContainer = AppColor.ErrorBackground,
        onErrorContainer = AppColor.Error,
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF5FCB8D),
        onPrimary = Color(0xFF063519),
        primaryContainer = Color(0xFF1E4A30),
        onPrimaryContainer = Color(0xFFB7EBC9),
        secondary = Color(0xFF8FD6AE),
        onSecondary = Color(0xFF0B3320),
        secondaryContainer = Color(0xFF24422F),
        onSecondaryContainer = Color(0xFFCBEBD8),
        tertiary = Color(0xFF7FB1FF),
        onTertiary = Color(0xFF00295C),
        tertiaryContainer = Color(0xFF003B7A),
        onTertiaryContainer = Color(0xFFD6E4FF),
        background = Color(0xFF0F1712),
        onBackground = Color(0xFFE7F0EA),
        surface = Color(0xFF16211B),
        onSurface = Color(0xFFE7F0EA),
        surfaceVariant = Color(0xFF24322B),
        onSurfaceVariant = Color(0xFFB9C6BF),
        outline = Color(0xFF3C4A42),
        outlineVariant = Color(0xFF2B3831),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF5C1620),
        errorContainer = Color(0xFF5C1620),
        onErrorContainer = Color(0xFFFFD8DB),
    )

/**
 * App-wide Material3 theme built from the brand palette above. Wrap the app's Compose content in
 * this once, at the root — every screen that reads MaterialTheme.colorScheme (cards, dialogs, the
 * snackbar, the debug FAB) then follows the brand automatically instead of the Material3 default.
 */
@Composable
fun CameraAccessTheme(content: @Composable () -> Unit) {
  val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
  MaterialTheme(colorScheme = colorScheme, content = content)
=======
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
>>>>>>> bbd53f3 (feat: implement UrbanSense AI app architecture with enhanced capture, detection services, and new dashboard navigation.)
}
