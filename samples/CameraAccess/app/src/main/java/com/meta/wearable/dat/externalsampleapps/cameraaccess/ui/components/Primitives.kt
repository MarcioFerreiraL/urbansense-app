/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.MinTouchTarget
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.UrbanSenseTheme

/**
 * The app's standard container. Every screen used to declare its own `Card` with slightly different
 * elevation, corner radius and padding, so no two screens agreed on what a card looked like.
 *
 * Elevation is deliberately flat: the theme separates `surface` from `surfaceVariant`, so cards read
 * as raised through contrast rather than through a shadow that disappears in dark mode anyway.
 */
@Composable
fun UsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(UrbanSenseTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
  val colors = CardDefaults.cardColors(containerColor = containerColor)
  val shape = MaterialTheme.shapes.medium
  val border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)

  if (onClick != null) {
    Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) {
      Column(modifier = Modifier.padding(contentPadding), content = content)
    }
  } else {
    Card(modifier = modifier, shape = shape, colors = colors, border = border) {
      Column(modifier = Modifier.padding(contentPadding), content = content)
    }
  }
}

/** Title, optional supporting line, and an optional trailing action, above a group of content. */
@Composable
fun UsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleLarge)
      if (subtitle != null) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (trailing != null) {
      Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
  }
}

/**
 * A small state pill. Replaces the two rival `StatusChip` implementations that lived in
 * `HistoryScreen` and `CameraScreen` with different signatures and different colour sources.
 */
@Composable
fun UsStatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
  Row(
      modifier =
          modifier
              .background(color = containerColor, shape = RoundedCornerShape(percent = 50))
              .padding(horizontal = UrbanSenseTheme.spacing.md, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.xs),
  ) {
    if (icon != null) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(14.dp),
      )
    }
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor)
  }
}

/** Primary call to action. Tall enough to hit while walking, which is the app's whole context. */
@Composable
fun UsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
  Button(
      onClick = onClick,
      modifier = modifier.defaultMinSize(minHeight = MinTouchTarget),
      enabled = enabled,
      shape = MaterialTheme.shapes.medium,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
      androidx.compose.foundation.layout.Spacer(Modifier.size(UrbanSenseTheme.spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

/** Secondary action, for anything that sits beside a [UsPrimaryButton]. */
@Composable
fun UsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.defaultMinSize(minHeight = MinTouchTarget),
      enabled = enabled,
      shape = MaterialTheme.shapes.medium,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      androidx.compose.foundation.layout.Spacer(Modifier.size(UrbanSenseTheme.spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

/** Centred icon + headline + explanation, with an optional call to action. */
@Composable
fun UsEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
  Column(
      modifier = modifier.fillMaxWidth().padding(UrbanSenseTheme.spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(UrbanSenseTheme.spacing.md),
  ) {
    Box(
        modifier =
            Modifier.size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(percent = 50),
                ),
        contentAlignment = Alignment.Center,
    ) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(34.dp),
      )
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      UsPrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
  }
}

/** A single figure with a caption, used in the dashboard's summary row. */
@Composable
fun UsStatTile(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
  UsCard(modifier = modifier, contentPadding = PaddingValues(UrbanSenseTheme.spacing.lg)) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
    )
    androidx.compose.foundation.layout.Spacer(Modifier.size(UrbanSenseTheme.spacing.sm))
    Text(text = value, style = MaterialTheme.typography.headlineSmall)
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
