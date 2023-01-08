package com.pyamsoft.tetherfi.tile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.theme.success
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.defaults.ImageDefaults
import com.pyamsoft.pydroid.ui.icons.RadioButtonUnchecked
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.delay
import timber.log.Timber

private val CONNECTOR_SIZE = 16.dp
private val STEP_SIZE = ImageDefaults.ItemSize

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

  val isError = remember(status) { status is RunningStatus.Error }

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
      modifier = modifier.padding(MaterialTheme.keylines.content),
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
              modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
          ) {
            StatusText(
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
                status = status,
            )
            ProgressStepper(
                modifier = Modifier.fillMaxWidth(),
                isError = isError,
                isMiddleStep = isMiddleStep,
                isFinalStep = isFinalStep,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatusText(
    modifier: Modifier = Modifier,
    status: RunningStatus,
) {
  val statusText =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> "ERROR"
          is RunningStatus.NotRunning -> "STOPPED"
          is RunningStatus.Running -> "RUNNING"
          is RunningStatus.Starting -> "STARTING..."
          is RunningStatus.Stopping -> "STOPPING..."
        }
      }

  Text(
      modifier = modifier,
      text = statusText,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.h4,
  )
}

@Composable
private fun ProgressStepper(
    modifier: Modifier = Modifier,
    isError: Boolean,
    isMiddleStep: Boolean,
    isFinalStep: Boolean,
) {
  val isFirstConnectorDone =
      remember(
          isMiddleStep,
          isFinalStep,
      ) {
        isMiddleStep || isFinalStep
      }

  val isFirstConnectorInProgress =
      remember(
          isMiddleStep,
          isFinalStep,
      ) {
        !isMiddleStep && !isFinalStep
      }

  val isSecondConnectorDone =
      remember(
          isMiddleStep,
          isFinalStep,
      ) {
        isMiddleStep && isFinalStep
      }

  val isSecondConnectorInProgress =
      remember(
          isMiddleStep,
          isFinalStep,
      ) {
        isMiddleStep && !isFinalStep
      }

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Step(
        isError = isError,
        isDone = true,
    )

    Connector(
        modifier = Modifier.weight(1F),
        isError = isError,
        isDone = isFirstConnectorDone,
        inProgress = isFirstConnectorInProgress,
    )

    Step(
        isError = isError,
        isDone = isFirstConnectorDone,
    )

    Connector(
        modifier = Modifier.weight(1F),
        isError = isError,
        isDone = isSecondConnectorDone,
        inProgress = isSecondConnectorInProgress,
    )

    Step(
        isError = isError,
        isDone = isSecondConnectorDone,
    )
  }
}

@Composable
private fun Step(
    modifier: Modifier = Modifier,
    isError: Boolean,
    isDone: Boolean,
) {
  val colors = MaterialTheme.colors
  val color =
      remember(
          isError,
          isDone,
          colors,
      ) {
        if (isError) {
          colors.error
        } else if (isDone) {
          colors.success
        } else {
          colors.onSurface
        }
      }

  Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        modifier = Modifier.size(STEP_SIZE),
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = color,
    )
  }
}

@Composable
private fun Connector(
    modifier: Modifier = Modifier,
    isError: Boolean,
    isDone: Boolean,
    inProgress: Boolean,
) {
  val colors = MaterialTheme.colors
  val color =
      remember(
          isError,
          isDone,
          inProgress,
          colors,
      ) {
        if (isError) {
          colors.error
        } else if (isDone) {
          colors.success
        } else if (inProgress) {
          colors.primary
        } else {
          colors.onSurface
        }
      }

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    repeat(3) {
      Icon(
          modifier = Modifier.size(CONNECTOR_SIZE),
          imageVector = Icons.Filled.RadioButtonUnchecked,
          contentDescription = null,
          tint = color,
      )
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
private fun PreviewProxyTileScreenInitial() {
  ProxyTileScreen(
      state = MutableProxyTileViewState(),
      onDismissed = {},
      onComplete = {},
      onStatusUpdated = {},
  )
}
