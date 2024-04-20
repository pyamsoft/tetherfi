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

package com.pyamsoft.tetherfi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import kotlinx.coroutines.delay

private enum class ServerErrorDialogContentTypes {
  TITLE,
  TRACE,
}

@Composable
fun ServerErrorTile(
    onShowError: () -> Unit,
    content: IconButtonContent,
) {
  /** Show the content which hosts the button (we delay slightly to avoid state-change flicker */
  val (showContent, setShowContent) = remember { mutableStateOf(false) }
  val handleShowContent by rememberUpdatedState { setShowContent(true) }

  val hapticManager = LocalHapticManager.current

  val handleClick by rememberUpdatedState {
    hapticManager?.actionButtonPress()
    onShowError()
  }

  LaunchedEffect(showContent) {
    if (!showContent) {
      // Wait a little bit in case the network is just starting up normally
      delay(1000L)

      // Mark the content as shown
      handleShowContent()
    }
  }

  AnimatedVisibility(
      visible = showContent,
  ) {
    content(
        Modifier.clickable { handleClick() },
    ) {
      IconButton(
          onClick = { handleClick() },
      ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Something went wrong",
            tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
fun ServerErrorDialog(
    modifier: Modifier = Modifier,
    title: String,
    error: Throwable,
    onDismiss: () -> Unit,
) {
  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = { onDismiss() },
  ) {
    Column(
        modifier =
            modifier
                // Top already has padding for some reason?
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
    ) {
      DialogToolbar(
          modifier = Modifier.fillMaxWidth(),
          onClose = onDismiss,
          title = {
            Text(
                text = "Hotspot Error",
            )
          },
      )
      Surface(
          modifier = Modifier.fillMaxWidth().weight(1F),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape =
              MaterialTheme.shapes.medium.copy(
                  topStart = ZeroCornerSize,
                  topEnd = ZeroCornerSize,
              ),
      ) {
        LazyColumn {
          item(
              contentType = ServerErrorDialogContentTypes.TITLE,
          ) {
            Text(
                modifier = Modifier.padding(MaterialTheme.keylines.content),
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
          }

          item(
              contentType = ServerErrorDialogContentTypes.TRACE,
          ) {
            val trace = remember(error) { error.stackTraceToString() }
            Text(
                modifier = Modifier.padding(MaterialTheme.keylines.content),
                text = trace,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
            )
          }
        }
      }
    }
  }
}
