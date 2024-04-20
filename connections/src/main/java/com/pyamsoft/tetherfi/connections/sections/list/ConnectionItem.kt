/*
 * Copyright 2024 pyamsoft
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

package com.pyamsoft.tetherfi.connections.sections.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.TypographyDefaults
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.clients.key
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val FIRST_SEEN_DATE_FORMATTER =
    object : ThreadLocal<DateTimeFormatter>() {

      override fun initialValue(): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedDateTime(
            FormatStyle.MEDIUM,
            FormatStyle.MEDIUM,
        )
      }
    }

@Composable
internal fun ConnectionItem(
    modifier: Modifier = Modifier,
    blocked: SnapshotStateList<TetherClient>,
    client: TetherClient,
    onClick: (TetherClient) -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val name = remember(client) { client.key() }
  val seenTime =
      remember(client) {
        FIRST_SEEN_DATE_FORMATTER.get().requireNotNull().format(client.mostRecentlySeen)
      }
  val totalTransferredToInternet = remember(client) { client.transferToInternet }
  val totalTransferredFromInternet = remember(client) { client.transferFromInternet }

  val isNotBlocked =
      remember(
          client,
          blocked,
      ) {
        val isBlocked = blocked.firstOrNull { it.key() == client.key() }
        return@remember isBlocked == null
      }

  Box(
      modifier =
          modifier
              .padding(horizontal = MaterialTheme.keylines.content)
              .padding(bottom = MaterialTheme.keylines.content),
  ) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
        shape = MaterialTheme.shapes.medium,
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            modifier = Modifier.weight(1F),
            text = name,
            style = MaterialTheme.typography.titleLarge,
        )
        Switch(
            checked = isNotBlocked,
            onCheckedChange = { newBlocked ->
              if (newBlocked) {
                hapticManager?.toggleOn()
              } else {
                hapticManager?.toggleOff()
              }
              onClick(client)
            },
        )
      }

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.typography),
          text = "Last seen: $seenTime",
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
      )

      Text(
          text = "Total Transferred To Internet: $totalTransferredToInternet",
          style =
              MaterialTheme.typography.bodySmall.copy(
                  color =
                      MaterialTheme.colorScheme.onSurface.copy(
                          alpha = TypographyDefaults.ALPHA_DISABLED,
                      ),
              ),
      )

      Text(
          text = "Total Transferred From Internet: $totalTransferredFromInternet",
          style =
              MaterialTheme.typography.bodySmall.copy(
                  color =
                      MaterialTheme.colorScheme.onSurface.copy(
                          alpha = TypographyDefaults.ALPHA_DISABLED,
                      ),
              ),
      )
    }
  }
}
