/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 4dp-based spacing scale. Screens were previously peppered with one-off `padding(13.dp)` style
 * values, which is why vertical rhythm drifted between them; reach for these instead.
 */
@Immutable
data class Spacing(
    /** 4dp — entre um ícone e seu rótulo. */
    val xs: Dp = 4.dp,
    /** 8dp — entre linhas de um mesmo bloco. */
    val sm: Dp = 8.dp,
    /** 12dp — padding interno de chips e cards compactos. */
    val md: Dp = 12.dp,
    /** 16dp — padding padrão de card e margem lateral de tela. */
    val lg: Dp = 16.dp,
    /** 24dp — entre seções. */
    val xl: Dp = 24.dp,
    /** 32dp — respiro antes de um CTA principal. */
    val xxl: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Altura mínima de alvo de toque recomendada pelo Material — cards e botões devem respeitá-la. */
val MinTouchTarget: Dp = 48.dp
