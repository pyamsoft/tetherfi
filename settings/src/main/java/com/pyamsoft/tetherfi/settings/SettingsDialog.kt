package com.pyamsoft.tetherfi.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.settings.SettingsPage

@Composable
fun SettingsDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
  Dialog(
      onDismissRequest = onDismiss,
  ) {
    Column(
        modifier = modifier.padding(MaterialTheme.keylines.content),
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
