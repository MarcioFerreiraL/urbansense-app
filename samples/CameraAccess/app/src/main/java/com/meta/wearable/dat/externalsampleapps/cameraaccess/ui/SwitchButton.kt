/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The app's primary/destructive full-width action button, used for every major CTA. */
@Composable
fun SwitchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
  val colors = MaterialTheme.colorScheme
  Button(
      modifier = modifier.height(56.dp).fillMaxWidth(),
      onClick = onClick,
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = if (isDestructive) colors.errorContainer else colors.primary,
              contentColor = if (isDestructive) colors.onErrorContainer else colors.onPrimary,
              disabledContainerColor = colors.surfaceVariant,
              disabledContentColor = colors.onSurfaceVariant,
          ),
      elevation =
          if (isDestructive) null
          else ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
      enabled = enabled,
  ) {
    Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
  }
}
