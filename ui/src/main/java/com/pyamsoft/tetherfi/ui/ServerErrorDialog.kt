/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import kotlinx.coroutines.delay

typealias IconButtonContent =
    @Composable
    (
        Modifier,
        @Composable () -> Unit,
    ) -> Unit

@Composable
fun GroupInfoErrorDialog(
    modifier: Modifier = Modifier,
    group: WiDiNetworkStatus.GroupInfo.Error,
    content: IconButtonContent,
) {
  ServerErrorDialog(
      modifier = modifier,
      title = "Hotspot Initialization Error",
      error = group.error,
      content = content,
  )
}

@Composable
fun ConnectionInfoErrorDialog(
    modifier: Modifier = Modifier,
    connection: WiDiNetworkStatus.ConnectionInfo.Error,
    content: IconButtonContent,
) {
  ServerErrorDialog(
      modifier = modifier,
      title = "Network Initialization Error",
      error = connection.error,
      content = content,
  )
}

@Composable
private fun ServerErrorDialog(
    modifier: Modifier,
    title: String,
    error: Throwable,
    content: IconButtonContent,
) {
  /** Show the Dialog */
  val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

  /** Show the content which hosts the button (we delay slightly to avoid state-change flicker */
  val (showContent, setShowContent) = remember { mutableStateOf(false) }
  val handleShowContent by rememberUpdatedState { setShowContent(true) }

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
        Modifier.clickable { setShowDialog(true) },
    ) {
      IconButton(
          onClick = { setShowDialog(true) },
      ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Something went wrong",
            tint = MaterialTheme.colors.error,
        )
      }
    }
  }

  if (showDialog && showContent) {
    val onDismiss by rememberUpdatedState { setShowDialog(false) }

    Dialog(
        properties = rememberDialogProperties(),
        onDismissRequest = { onDismiss() },
    ) {
      Column(
          modifier = modifier.padding(MaterialTheme.keylines.content),
      ) {
        DialogToolbar(
            modifier = Modifier.fillMaxWidth(),
            onClose = { onDismiss() },
            title = {
              Text(
                  text = "Hotspot Error",
              )
            },
        )
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1F),
            shape =
                MaterialTheme.shapes.medium.copy(
                    topStart = ZeroCornerSize,
                    topEnd = ZeroCornerSize,
                ),
        ) {
          LazyColumn {
            item {
              Text(
                  modifier = Modifier.padding(MaterialTheme.keylines.content),
                  text = title,
                  style = MaterialTheme.typography.h6,
              )
            }

            item {
              val trace = remember(error) { error.stackTraceToString() }
              Text(
                  modifier = Modifier.padding(MaterialTheme.keylines.content),
                  text = trace,
                  style =
                      MaterialTheme.typography.caption.copy(
                          fontFamily = FontFamily.Monospace,
                      ),
              )
            }
          }
        }
      }
    }
  }
}
