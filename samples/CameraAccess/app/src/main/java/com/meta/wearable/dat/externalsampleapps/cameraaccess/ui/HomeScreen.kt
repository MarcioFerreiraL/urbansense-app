/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// HomeScreen - DAT Registration Entry Point
//
// This screen handles DAT device registration.

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun HomeScreen(
    viewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  val context = LocalContext.current
  val errorActivityUnavailable = stringResource(R.string.error_activity_unavailable)

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(all = 24.dp)
              .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    // Caps the content width on large screens (tablets) so the layout doesn't stretch edge to
    // edge; stays full-width on phones.
    Column(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      androidx.compose.foundation.Image(
          painter = painterResource(id = R.drawable.ic_urbansense_logo),
          contentDescription = stringResource(R.string.camera_access_icon_description),
          modifier = Modifier.size(140.dp).clip(androidx.compose.foundation.shape.CircleShape),
      )
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Image(
            painter = painterResource(id = R.drawable.logo_urbansense),
            contentDescription = stringResource(R.string.app_logo_description),
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)),
        )
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
          Column(
              verticalArrangement = Arrangement.spacedBy(18.dp),
              modifier = Modifier.fillMaxWidth().padding(18.dp),
          ) {
            TipRow(
                iconResId = R.drawable.smart_glasses_icon,
                title = stringResource(R.string.home_tip_session_title),
                text = stringResource(R.string.home_tip_session),
            )
            TipRow(
                iconResId = R.drawable.video_icon,
                title = stringResource(R.string.home_tip_preview_title),
                text = stringResource(R.string.home_tip_preview),
            )
            TipRow(
                iconResId = R.drawable.tap_icon,
                title = stringResource(R.string.home_tip_capture_title),
                text = stringResource(R.string.home_tip_capture),
            )
          }
        }
      }
      Spacer(modifier = Modifier.weight(1f))

      Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // App Registration Button
        Text(
            text = stringResource(R.string.home_redirect_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        SwitchButton(
            label = stringResource(R.string.register_button_title),
            enabled = uiState.canStartRegistration,
            onClick = {
              activity?.let { viewModel.startRegistration(it) }
                  ?: Toast.makeText(context, errorActivityUnavailable, Toast.LENGTH_SHORT).show()
            },
        )
      }
    }
  }
}
