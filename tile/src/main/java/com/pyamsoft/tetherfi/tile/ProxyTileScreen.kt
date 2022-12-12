package com.pyamsoft.tetherfi.tile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.delay

@Composable
fun ProxyTileScreen(
    modifier: Modifier = Modifier,
    state: ProxyTileViewState,
    onDismissed: () -> Unit,
    onComplete: () -> Unit,
) {
  // Capture this once, the first time
  val status = state.status
  val initialStatus = remember { status }
  val isInitialStatusError = remember(initialStatus) { initialStatus is RunningStatus.Error }

  val isShowing = state.isShowing

  // Once the dialog is flagged off, we fire this "completed" hook
  LaunchedEffect(
      isShowing,
      onComplete,
  ) {
    if (!isShowing) {
      onComplete()
    }
  }

  LaunchedEffect(
      initialStatus,
      status,
      onDismissed,
  ) {
    // If the current status is an error
    if (status is RunningStatus.Error) {
      // display the error message for ~2 seconds and then dismiss
      delay(2_000)
      onDismissed()
      return@LaunchedEffect
    }

    // status has changed
    if (initialStatus != status) {
      // If we started not running and now we are running
      val isInitialStarting =
          initialStatus is RunningStatus.NotRunning || initialStatus is RunningStatus.Starting
      if (isInitialStarting && status is RunningStatus.Running) {
        // display for ~1 second and dismiss
        delay(1_000)
        onDismissed()
        return@LaunchedEffect
      }

      // If we started running and now we are stopped
      val isInitialStopping =
          initialStatus is RunningStatus.Running || initialStatus is RunningStatus.Stopping
      if (isInitialStopping && status is RunningStatus.NotRunning) {
        // display for ~1 second and dismiss
        delay(1_000)
        onDismissed()
        return@LaunchedEffect
      }
    }
  }

  Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
  ) {
    if (isShowing) {
      Dialog(
          onDismissRequest = onDismissed,
          properties =
              DialogProperties(
                  dismissOnBackPress = isInitialStatusError,
                  dismissOnClickOutside = isInitialStatusError,
              ),
      ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = DialogDefaults.Elevation,
        ) {
          Box(
              modifier = Modifier.padding(MaterialTheme.keylines.content),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text =
                    "Status: ${when (status) {
                          is RunningStatus.Error -> "ERROR"
                          is RunningStatus.NotRunning -> "STOPPED"
                          is RunningStatus.Running -> "RUNNING"
                          is RunningStatus.Starting -> "STARTING..."
                          is RunningStatus.Stopping -> "STOPPING..."
                      }
                      }",
                style = MaterialTheme.typography.h6,
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun PreviewProxyTileScreen() {
  ProxyTileScreen(
      state = MutableProxyTileViewState(),
      onDismissed = {},
      onComplete = {},
  )
}
