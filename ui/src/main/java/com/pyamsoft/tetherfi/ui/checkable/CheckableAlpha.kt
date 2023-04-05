package com.pyamsoft.tetherfi.ui.checkable

import androidx.annotation.CheckResult
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal data class CheckableCardAlpha
internal constructor(
    val primary: Float,
    val secondary: Float,
)

@Composable
@CheckResult
internal fun rememberCheckableAlpha(isEditable: Boolean): CheckableCardAlpha {
  val highAlpha = ContentAlpha.high
  val mediumAlpha = ContentAlpha.medium
  val disabledAlpha = ContentAlpha.disabled

  return remember(
      isEditable,
      highAlpha,
      mediumAlpha,
      disabledAlpha,
  ) {
    val primary = if (isEditable) highAlpha else disabledAlpha
    val secondary = if (isEditable) mediumAlpha else disabledAlpha

    return@remember CheckableCardAlpha(primary, secondary)
  }
}
