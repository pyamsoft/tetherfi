package com.pyamsoft.tetherfi.tile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun ProxyTileScreen(
    modifier: Modifier = Modifier,
    state: ProxyTileViewState,
    onDismissed: () -> Unit,
    onComplete: () -> Unit,
    onStatusUpdated: (RunningStatus) -> Unit,
) {
  // Capture this once, the first time
  val status = state.status
  val initialStatus = remember { status }
  val isInitialStatusError = remember(initialStatus) { initialStatus is RunningStatus.Error }

  val isShowing = state.isShowing

  val isMiddleStep =
      remember(
          status,
          initialStatus,
      ) {
        when (initialStatus) {
          is RunningStatus.NotRunning -> {
            // We are at least middle if we are starting or started
            return@remember status is RunningStatus.Starting || status is RunningStatus.Running
          }
          is RunningStatus.Running -> {
            // We are at least middle if we are stopping or stopped
            return@remember status is RunningStatus.Stopping || status is RunningStatus.NotRunning
          }
          else -> {
            Timber.w("Unexpected middle step, we are in progress so mark us as middle: $status")
            return@remember true
          }
        }
      }

  val isFinalStep =
      remember(
          status,
          initialStatus,
      ) {
        when (initialStatus) {
          is RunningStatus.NotRunning -> {
            // We are done if we are started
            return@remember status is RunningStatus.Running
          }
          is RunningStatus.Running -> {
            // We are at done if we are stopped
            return@remember status is RunningStatus.NotRunning
          }
          else -> {
            Timber.w(
                "Unexpected final step, we are in progress so do not mark us as final: $status")
            return@remember false
          }
        }
      }

  SideEffectUpdate(
      status = status,
      onStatusUpdated = onStatusUpdated,
  )
  SideEffectComplete(
      isShowing = isShowing,
      onComplete = onComplete,
  )
  SideEffectStep(
      initialStatus = initialStatus,
      status = status,
      onDismissed = onDismissed,
  )

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
          Column(
              modifier = Modifier.padding(MaterialTheme.keylines.content),
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

            Text(
                text = "First: YES",
                style = MaterialTheme.typography.body1,
            )

            Text(
                text = "Middle: ${if (isMiddleStep) "YES" else "NO"}",
                style = MaterialTheme.typography.body1,
            )

            Text(
                text = "Final: ${if (isFinalStep) "YES" else "NO"}",
                style = MaterialTheme.typography.body1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SideEffectStep(
    initialStatus: RunningStatus,
    status: RunningStatus,
    onDismissed: () -> Unit,
) {
  // Don't use by so we can memoize
  val handleDismissed = rememberUpdatedState(onDismissed)

  LaunchedEffect(
      initialStatus,
      status,
      handleDismissed,
  ) {
    // If the current status is an error
    if (status is RunningStatus.Error) {
      // display the error message for ~2 seconds and then dismiss
      delay(2_000)
      handleDismissed.value.invoke()
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
        handleDismissed.value.invoke()
        return@LaunchedEffect
      }

      // If we started running and now we are stopped
      val isInitialStopping =
          initialStatus is RunningStatus.Running || initialStatus is RunningStatus.Stopping
      if (isInitialStopping && status is RunningStatus.NotRunning) {
        // display for ~1 second and dismiss
        delay(1_000)
        handleDismissed.value.invoke()
        return@LaunchedEffect
      }
    }
  }
}

@Composable
private fun SideEffectUpdate(
    status: RunningStatus,
    onStatusUpdated: (RunningStatus) -> Unit,
) {
  // Don't use by so we can memoize
  val handleStatusUpdated = rememberUpdatedState(onStatusUpdated)
  LaunchedEffect(
      status,
      handleStatusUpdated,
  ) {
    handleStatusUpdated.value.invoke(status)
  }
}

@Composable
private fun SideEffectComplete(
    isShowing: Boolean,
    onComplete: () -> Unit,
) {

  // Don't use by so we can memoize
  val handleCompleted = rememberUpdatedState(onComplete)
  LaunchedEffect(
      isShowing,
      handleCompleted,
  ) {
    // Once the dialog is flagged off, we fire this "completed" hook
    if (!isShowing) {
      handleCompleted.value.invoke()
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
      onStatusUpdated = {},
  )
}
