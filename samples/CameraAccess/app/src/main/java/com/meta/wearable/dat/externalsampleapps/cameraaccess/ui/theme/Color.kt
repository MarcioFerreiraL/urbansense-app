/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette, sampled from the UrbanSense logo — the green "U" enclosing a city skyline with
 * a white path running through it.
 *
 * These are the source-of-truth hues. Screens should not reference them directly: they read
 * [androidx.compose.material3.MaterialTheme.colorScheme] or [SemanticColors] instead, so that light
 * and dark themes stay coherent. The exceptions are brand fills that carry no text on top (the logo
 * itself, illustrative shapes).
 */
object Brand {
  /** Fundo e o "caminho" que atravessa o logo. */
  val White = Color(0xFFFFFFFF)

  /** Sombras na base do "U". Contraste 7.9:1 com branco. */
  val GreenDeep = Color(0xFF295C33)

  /**
   * Corpo principal do "U" e dos arranha-céus — a cor da marca.
   *
   * Contraste com branco é apenas 4.35:1, abaixo do mínimo de 4.5:1 do WCAG AA para texto normal.
   * Por isso ela NÃO é usada como `primary`: fica reservada para preenchimentos ilustrativos, onde
   * não há texto por cima. Para superfícies interativas com rótulo, use [GreenAction].
   */
  val GreenCore = Color(0xFF488746)

  /** Árvores e colinas médias. */
  val GreenGrass = Color(0xFF5D9C56)

  /** Áreas iluminadas das colinas. */
  val GreenLight = Color(0xFF7FAD6C)

  /** Destaques mais claros na grama. */
  val GreenSoft = Color(0xFFA3C893)

  /**
   * [GreenCore] escurecido ~8% para atingir 5.9:1 com branco — passa em WCAG AA para texto normal.
   * É a cor de ação do app: botões, switches ligados, ícones ativos.
   */
  val GreenAction = Color(0xFF37703F)
}

/**
 * Semantic states that Material 3's [androidx.compose.material3.ColorScheme] has no slot for.
 *
 * A marca é monocromática, então "sucesso" compartilha a matiz verde (é o estado feliz do app), mas
 * aviso, erro e informação precisam de matizes próprias — senão todo estado vira o mesmo verde e o
 * usuário perde a leitura rápida de um relatório que falhou.
 *
 * Fornecido via [LocalSemanticColors] para que troque junto com o tema claro/escuro.
 */
@androidx.compose.runtime.Immutable
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    /** Gravação em andamento. Vermelho universal — independe da marca de propósito. */
    val recordAccent: Color,
    /** Cor do pino no mapa, por status do registro. */
    val pinProcessed: Color,
    val pinQueued: Color,
    val pinPending: Color,
    val pinFailed: Color,
)

internal val LightSemanticColors =
    SemanticColors(
        success = Brand.GreenAction,
        onSuccess = Color.White,
        successContainer = Color(0xFFE6F1E1),
        onSuccessContainer = Color(0xFF1B3F22),
        warning = Color(0xFF8A5A00),
        onWarning = Color.White,
        warningContainer = Color(0xFFFFF1D6),
        onWarningContainer = Color(0xFF4A3000),
        info = Color(0xFF1F5F8B),
        onInfo = Color.White,
        infoContainer = Color(0xFFE0EDF5),
        onInfoContainer = Color(0xFF0E3450),
        recordAccent = Color(0xFFE03131),
        pinProcessed = Brand.GreenDeep,
        pinQueued = Brand.GreenGrass,
        pinPending = Color(0xFF8A5A00),
        pinFailed = Color(0xFF9B1C1C),
    )

internal val DarkSemanticColors =
    SemanticColors(
        success = Color(0xFF8FC98A),
        onSuccess = Color(0xFF10300F),
        successContainer = Color(0xFF2C5730),
        onSuccessContainer = Color(0xFFC6E3BF),
        warning = Color(0xFFE8B75C),
        onWarning = Color(0xFF3D2800),
        warningContainer = Color(0xFF5A3E00),
        onWarningContainer = Color(0xFFFFE0A8),
        info = Color(0xFF87BEDE),
        onInfo = Color(0xFF0E3450),
        infoContainer = Color(0xFF1B4966),
        onInfoContainer = Color(0xFFCEE5F2),
        recordAccent = Color(0xFFFF5A5A),
        pinProcessed = Brand.GreenLight,
        pinQueued = Brand.GreenGrass,
        pinPending = Color(0xFFE8B75C),
        pinFailed = Color(0xFFFF6B6B),
    )

/**
 * Fixed tokens for the camera viewfinder.
 *
 * `CameraScreen` deliberately keeps a dark chrome regardless of the system theme — a viewfinder with
 * a white surround washes out the image and blinds the user at night. It therefore cannot read
 * `MaterialTheme.colorScheme`, whose light palette would be illegible there. These are the brand
 * colours picked for legibility on black, kept as named tokens so they are still part of the design
 * system rather than literals scattered through the screen.
 */
object ViewfinderColor {
  val ActiveDot = Brand.GreenLight
  val PendingDot = Color(0xFFE8B75C)
  val InactiveDot = Color(0xFF8A968C)
  val RecordAccent = Color(0xFFFF5A5A)
  val WarningContainer = Color(0xFF5A3E00)
  val OnWarningContainer = Color(0xFFFFE0A8)
}
