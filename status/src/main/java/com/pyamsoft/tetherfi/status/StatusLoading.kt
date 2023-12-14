package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusLoading(
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
        modifier = Modifier.size(120.dp),
    )
  }
}
