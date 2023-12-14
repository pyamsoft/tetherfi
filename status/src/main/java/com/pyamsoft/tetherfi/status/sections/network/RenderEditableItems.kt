package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.StatusViewState

private enum class RenderEditableItemsContentTypes {
  EDIT_SSID,
  EDIT_PASSWD,
  EDIT_PORT,
}

internal fun LazyListScope.renderEditableItems(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item(
      contentType = RenderEditableItemsContentTypes.EDIT_SSID,
  ) {
    EditSsid(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onSsidChanged = onSsidChanged,
    )
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_PASSWD,
  ) {
    EditPassword(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onPasswordChanged = onPasswordChanged,
    )
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_PORT,
  ) {
    EditPort(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onPortChanged = onPortChanged,
    )
  }
}
