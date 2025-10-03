/*
 * Copyright 2025 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status.sections.tiles

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.ui.dialog.ServerErrorTile

@Composable
internal fun GroupErrorTile(
    modifier: Modifier = Modifier,
    group: BroadcastNetworkStatus.GroupInfo,
    onShowGroupError: () -> Unit,
) {
  group.cast<BroadcastNetworkStatus.GroupInfo.Error>()?.also {
    StatusTile(
        modifier = modifier,
        borderColor = MaterialTheme.colorScheme.error,
    ) {
      ServerErrorTile(
          onShowError = onShowGroupError,
      ) { modifier, iconButton ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          val color = LocalContentColor.current

          iconButton()

          Text(
              text = stringResource(R.string.tile_hotspot_error),
              style =
                  MaterialTheme.typography.bodySmall.copy(
                      color = color,
                  ),
          )
        }
      }
    }
  }
}
