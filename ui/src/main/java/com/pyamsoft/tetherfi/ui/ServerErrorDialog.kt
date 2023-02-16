package com.pyamsoft.tetherfi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus

@Composable
fun GroupInfoErrorDialog(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    group: WiDiNetworkStatus.GroupInfo,
) {
  (group as? WiDiNetworkStatus.GroupInfo.Error)?.also { err ->
    ServerErrorDialog(
        modifier = modifier,
        iconModifier = iconModifier,
        dialogModifier = dialogModifier,
        title = "Group Info Error",
        error = err.error,
    )
  }
}

@Composable
fun ConnectionInfoErrorDialog(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    connection: WiDiNetworkStatus.ConnectionInfo,
) {
  (connection as? WiDiNetworkStatus.ConnectionInfo.Error)?.also { err ->
    ServerErrorDialog(
        modifier = modifier,
        iconModifier = iconModifier,
        dialogModifier = dialogModifier,
        title = "Connection Info Error",
        error = err.error,
    )
  }
}

@Composable
private fun ServerErrorDialog(
    modifier: Modifier,
    iconModifier: Modifier,
    dialogModifier: Modifier,
    title: String,
    error: Throwable,
) {
  val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

  // Icon to click on to show the dialog
  // Don't use IconButton because we don't care about minimum touch target size
  Box(
      modifier = modifier.clickable { setShowDialog(true) },
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        modifier = iconModifier,
        imageVector = Icons.Filled.Warning,
        contentDescription = "Something went wrong",
        tint = MaterialTheme.colors.error,
    )
  }

  if (showDialog) {
    val onDismiss by rememberUpdatedState { setShowDialog(false) }

    Dialog(
        properties = rememberDialogProperties(),
        onDismissRequest = { onDismiss() },
    ) {
      Column(
          modifier = dialogModifier.padding(MaterialTheme.keylines.content),
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
