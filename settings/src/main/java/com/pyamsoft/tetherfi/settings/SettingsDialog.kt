package com.pyamsoft.tetherfi.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.PaddedDialog
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.settings.SettingsPage

@Composable
fun SettingsDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
  PaddedDialog(
      onDismissRequest = onDismiss,
  ) {
    Column(
        modifier = modifier,
    ) {
      SettingsToolbar(
          modifier = Modifier.fillMaxWidth(),
          onClose = onDismiss,
      )
      SettingsPage(
          modifier = Modifier.fillMaxWidth().weight(1F),
          customElevation = DialogDefaults.Elevation,
          customBottomItemMargin = MaterialTheme.keylines.baseline,
          shape =
              MaterialTheme.shapes.medium.copy(
                  topStart = ZeroCornerSize,
                  topEnd = ZeroCornerSize,
              ),
      )
    }
  }
}
