/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Brand.GreenAction,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE9D4),
        onPrimaryContainer = Color(0xFF1B3F22),
        secondary = Brand.GreenGrass,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8F0E1),
        onSecondaryContainer = Brand.GreenDeep,
        tertiary = Color(0xFF1F5F8B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0EDF5),
        onTertiaryContainer = Color(0xFF0E3450),
        background = Color(0xFFF6F8F4),
        onBackground = Color(0xFF14261A),
        surface = Color.White,
        onSurface = Color(0xFF14261A),
        // Deliberately distinct from `surface`: the previous theme set both to pure white, which
        // made every card blend into the sheet it sat on.
        surfaceVariant = Color(0xFFEDF2E8),
        onSurfaceVariant = Color(0xFF4A5A4C),
        outline = Color(0xFFC3D2BB),
        outlineVariant = Color(0xFFDEE7D8),
        error = Color(0xFF9B1C1C),
        onError = Color.White,
        errorContainer = Color(0xFFFDE4E4),
        onErrorContainer = Color(0xFF5F0F0F),
        scrim = Color(0xFF000000),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8FC98A),
        onPrimary = Color(0xFF10300F),
        primaryContainer = Color(0xFF2C5730),
        onPrimaryContainer = Color(0xFFC6E3BF),
        secondary = Brand.GreenSoft,
        onSecondary = Color(0xFF1B3F22),
        secondaryContainer = Color(0xFF33513A),
        onSecondaryContainer = Color(0xFFD3E7CB),
        tertiary = Color(0xFF87BEDE),
        onTertiary = Color(0xFF0E3450),
        tertiaryContainer = Color(0xFF1B4966),
        onTertiaryContainer = Color(0xFFCEE5F2),
        background = Color(0xFF101711),
        onBackground = Color(0xFFE4EDE0),
        surface = Color(0xFF17211A),
        onSurface = Color(0xFFE4EDE0),
        surfaceVariant = Color(0xFF26332A),
        onSurfaceVariant = Color(0xFFBCC9B8),
        outline = Color(0xFF3E4B40),
        outlineVariant = Color(0xFF2C382F),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF4A0F0F),
        errorContainer = Color(0xFF5F1A1A),
        onErrorContainer = Color(0xFFFFD8D8),
        scrim = Color(0xFF000000),
    )

internal val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

/**
 * App-wide Material 3 theme. Wrap the whole app in this once, at the root — every screen that reads
 * `MaterialTheme.colorScheme` then follows the brand automatically.
 *
 * Deliberately does *not* opt into Material You dynamic color: this is a civic reporting tool where
 * the green identity carries meaning (it is the brand, and green also reads as "sent/OK" in the
 * status chips), so letting the wallpaper repaint it would be a regression.
 */
@Composable
fun UrbanSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColors else LightColors
  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

  CompositionLocalProvider(
      LocalSemanticColors provides semanticColors,
      LocalSpacing provides Spacing(),
  ) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = UrbanSenseTypography,
        shapes = UrbanSenseShapes,
        content = content,
    )
  }
}

/**
 * Accessors for the tokens Material 3 has no slot for, mirroring how `MaterialTheme.colorScheme`
 * works: `UrbanSenseTheme.semantic.warning`, `UrbanSenseTheme.spacing.lg`.
 */
object UrbanSenseTheme {
  val semantic: SemanticColors
    @Composable @ReadOnlyComposable get() = LocalSemanticColors.current

  val spacing: Spacing
    @Composable @ReadOnlyComposable get() = LocalSpacing.current
}
