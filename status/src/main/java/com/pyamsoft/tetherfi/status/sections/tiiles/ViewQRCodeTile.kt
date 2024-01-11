/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status.sections.tiiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.ui.icons.QrCode

@Composable
internal fun ViewQRCodeTile(
    modifier: Modifier = Modifier,
    isQREnabled: Boolean,
    onShowQRCode: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  StatusTile(
      modifier = modifier,
      enabled = isQREnabled,
  ) {
    Row(
        modifier =
            Modifier.fillMaxWidth().clickable(enabled = isQREnabled) {
              hapticManager?.actionButtonPress()
              onShowQRCode()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      val color = LocalContentColor.current

      IconButton(
          onClick = {
            hapticManager?.actionButtonPress()
            onShowQRCode()
          },
          enabled = isQREnabled,
      ) {
        Icon(
            imageVector = Icons.Filled.QrCode,
            contentDescription = "QR Code",
            tint =
                MaterialTheme.colors.primary.copy(
                    alpha = if (isQREnabled) ContentAlpha.high else ContentAlpha.disabled,
                ),
        )
      }

      Text(
          text = "View QR Code",
          style =
              MaterialTheme.typography.caption.copy(
                  color =
                      color.copy(
                          alpha = if (isQREnabled) ContentAlpha.medium else ContentAlpha.disabled,
                      ),
              ),
      )
    }
  }
}
