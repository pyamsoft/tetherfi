package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.server.status.RunningStatus

@Composable
internal fun HotspotStatus(
    modifier: Modifier = Modifier,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,
    hotspotStatus: RunningStatus,
    onShowBroadcastError: () -> Unit,
    onShowProxyError: () -> Unit,
) {
  Card(
      modifier = modifier,
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
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

      Box(
          modifier = Modifier.fillMaxWidth(),
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
