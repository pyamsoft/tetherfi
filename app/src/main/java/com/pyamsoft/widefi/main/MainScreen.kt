package com.pyamsoft.widefi.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines

@JvmOverloads
@Composable
internal fun MainScreen(
    modifier: Modifier = Modifier,
    state: MainViewState,
    onToggle: () -> Unit,
    onTabUpdated: (MainView) -> Unit,
) {

  Scaffold(
      modifier = modifier,
  ) {
    TopInfoSection(
        modifier = Modifier.padding(MaterialTheme.keylines.content),
        state = state,
        onToggle = onToggle,
        onTabUpdated = onTabUpdated,
    )
  }
}
