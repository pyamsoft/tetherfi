package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.BetterSurface
import com.pyamsoft.tetherfi.ui.bottomBorder
import com.pyamsoft.tetherfi.ui.topBorder

private enum class RenderHotspotStatusContentTypes {
  COMPONENT_STATUS,
  HOTSPOT_STATUS
}

internal fun LazyListScope.renderHotspotStatus(
    itemModifier: Modifier = Modifier,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,
    hotspotStatus: RunningStatus,
    onShowBroadcastError: () -> Unit,
    onShowProxyError: () -> Unit,
) {
  item(
      contentType = RenderHotspotStatusContentTypes.COMPONENT_STATUS,
  ) {
    BetterSurface(
        modifier =
            itemModifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.keylines.content * 2)
                .topBorder(
                    strokeWidth = 4.dp,
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.disabled,
                        ),
                    cornerRadius = MaterialTheme.keylines.content,
                ),
        elevation = DialogDefaults.Elevation,
        shape =
            MaterialTheme.shapes.medium.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        DisplayStatus(
            modifier = Modifier.weight(1F, fill = false),
            title = "Broadcast Status:",
            status = wiDiStatus,
            size = StatusSize.SMALL,
            onClickShowError = onShowBroadcastError,
        )

        DisplayStatus(
            modifier = Modifier.weight(1F, fill = false),
            title = "Proxy Status:",
            status = proxyStatus,
            size = StatusSize.SMALL,
            onClickShowError = onShowProxyError,
        )
      }
    }
  }

  item(
      contentType = RenderHotspotStatusContentTypes.HOTSPOT_STATUS,
  ) {
    BetterSurface(
        modifier =
            itemModifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.keylines.content * 2)
                .bottomBorder(
                    strokeWidth = 4.dp,
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.disabled,
                        ),
                    cornerRadius = MaterialTheme.keylines.content,
                ),
        elevation = DialogDefaults.Elevation,
        shape =
            MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize,
            ),
    ) {
      Box(
          modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
          contentAlignment = Alignment.Center,
      ) {
        DisplayStatus(
            title = "Hotspot Status:",
            status = hotspotStatus,
            size = StatusSize.NORMAL,
        )
      }
    }
  }
}
