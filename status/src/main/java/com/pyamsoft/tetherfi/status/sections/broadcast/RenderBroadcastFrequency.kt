package com.pyamsoft.tetherfi.status.sections.broadcast

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.status.StatusViewState

private enum class RenderBroadcastFrequencyContentTypes {
  BANDS
}

internal fun LazyListScope.renderBroadcastFrequency(
    itemModifier: Modifier = Modifier,
    state: StatusViewState,
    isEditable: Boolean,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  item(
      contentType = RenderBroadcastFrequencyContentTypes.BANDS,
  ) {
    NetworkBands(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        isEditable = isEditable,
        state = state,
        onSelectBand = onSelectBand,
    )
  }
}
