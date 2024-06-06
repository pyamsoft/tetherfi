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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.PopupProperties
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.TypographyDefaults
import com.pyamsoft.pydroid.ui.haptics.HapticManager
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.connections.R
import com.pyamsoft.tetherfi.server.clients.BandwidthLimit
import com.pyamsoft.tetherfi.server.clients.BandwidthUnit
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.clients.key
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.jetbrains.annotations.TestOnly

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
    onToggleBlock: (TetherClient) -> Unit,
    onManageNickName: (TetherClient) -> Unit,
    onManageBandwidthLimit: (TetherClient) -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val key = remember(client) { client.key() }
  val seenTime =
      remember(client) {
        FIRST_SEEN_DATE_FORMATTER.get().requireNotNull().format(client.mostRecentlySeen)
      }
  val isOverLimit = remember(client) { client.isOverBandwidthLimit() }

  val isNotBlocked =
      remember(
          client,
          blocked,
      ) {
        val isBlocked = blocked.firstOrNull { it.key() == client.key() }
        return@remember isBlocked == null
      }

  Box(
      modifier = modifier.padding(bottom = MaterialTheme.keylines.content),
  ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
      Column(
          modifier = Modifier.padding(MaterialTheme.keylines.content),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
              modifier = Modifier.weight(1F),
          ) {
            Name(
                client = client,
                key = key,
            )
          }

          OptionsMenu(
              client = client,
              hapticManager = hapticManager,
              onManageNickName = onManageNickName,
              onManageBandwidthLimit = onManageBandwidthLimit,
          )

          Switch(
              checked = isNotBlocked,
              onCheckedChange = { newBlocked ->
                if (newBlocked) {
                  hapticManager?.toggleOn()
                } else {
                  hapticManager?.toggleOff()
                }
                onToggleBlock(client)
              },
          )
        }

        Text(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.typography),
            text = stringResource(R.string.connection_last_seen, seenTime),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )

        Bandwidth(
            client = client,
            isOverLimit = isOverLimit,
        )
      }
    }
  }
}

@Composable
private fun OptionsMenu(
    client: TetherClient,
    hapticManager: HapticManager?,
    onManageNickName: (TetherClient) -> Unit,
    onManageBandwidthLimit: (TetherClient) -> Unit,
) {
  val (show, setShow) = remember { mutableStateOf(false) }

  val handleDismiss by rememberUpdatedState { setShow(false) }

  IconButton(
      modifier = Modifier.padding(horizontal = MaterialTheme.keylines.baseline),
      onClick = { hapticManager?.actionButtonPress() },
  ) {
    Icon(
        contentDescription = stringResource(R.string.connection_options),
        imageVector = Icons.Filled.MoreVert,
    )
  }

  DropdownMenu(
      expanded = show,
      properties = remember { PopupProperties(focusable = true) },
      onDismissRequest = { handleDismiss() },
  ) {
    DropdownMenuItem(
        onClick = {
          hapticManager?.actionButtonPress()
          handleDismiss()
          onManageNickName(client)
        },
        text = {
          Text(
              text = "Set Nick Name",
              style = MaterialTheme.typography.bodyMedium,
          )
        })

    DropdownMenuItem(
        onClick = {
          hapticManager?.actionButtonPress()
          handleDismiss()
          onManageBandwidthLimit(client)
        },
        text = {
          Text(
              text = "Set Bandwidth Limit",
              style = MaterialTheme.typography.bodyMedium,
          )
        })
  }
}

@Composable
private fun Name(
    client: TetherClient,
    key: String,
) {
  client.nickName.also { nickName ->
    val name = remember(nickName, key) { nickName.ifBlank { key } }
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
    )

    if (nickName.isNotBlank()) {
      Text(
          text = key,
          style = MaterialTheme.typography.bodySmall,
          color =
              MaterialTheme.colorScheme.onSurfaceVariant.copy(
                  alpha = TypographyDefaults.ALPHA_DISABLED,
              ),
      )
    }
  }
}

@Composable
private fun Bandwidth(
    client: TetherClient,
    isOverLimit: Boolean,
) {
  client.limit?.also { limit ->
    val displayLimit =
        remember(isOverLimit, limit) {
          if (isOverLimit) {
            "OVER LIMIT: ${limit.display}"
          } else {
            limit.display
          }
        }

    val color =
        if (isOverLimit) {
          MaterialTheme.colorScheme.error
        } else {
          MaterialTheme.colorScheme.onSurface.copy(
              alpha = TypographyDefaults.ALPHA_DISABLED,
          )
        }

    Text(
        text =
            stringResource(
                R.string.bandwidth_limit,
                displayLimit,
            ),
        style =
            MaterialTheme.typography.bodySmall.copy(
                color = color,
            ),
    )
  }

  Text(
      text =
          stringResource(R.string.connection_total_to_internet, client.transferToInternet.display),
      style =
          MaterialTheme.typography.bodySmall.copy(
              color =
                  MaterialTheme.colorScheme.onSurface.copy(
                      alpha = TypographyDefaults.ALPHA_DISABLED,
                  ),
          ),
  )

  Text(
      text =
          stringResource(
              R.string.connection_total_from_internet, client.transferFromInternet.display),
      style =
          MaterialTheme.typography.bodySmall.copy(
              color =
                  MaterialTheme.colorScheme.onSurface.copy(
                      alpha = TypographyDefaults.ALPHA_DISABLED,
                  ),
          ),
  )
}

@TestOnly
@Composable
private fun PreviewConnectionItem(
    nickName: String,
    limit: BandwidthLimit?,
    totalBytes: ByteTransferReport,
) {
  ConnectionItem(
      blocked = remember { mutableStateListOf() },
      client =
          TetherClient.testCreate(
              hostNameOrIp = "127.0.0.1",
              clock = Clock.systemDefaultZone(),
              nickName = nickName,
              limit = limit,
              totalBytes = totalBytes,
          ),
      onToggleBlock = {},
      onManageBandwidthLimit = {},
      onManageNickName = {},
  )
}

@Preview
@Composable
private fun PreviewConnectionItemDefault() {
  PreviewConnectionItem(
      nickName = "",
      limit = null,
      totalBytes = ByteTransferReport.EMPTY,
  )
}

@Preview
@Composable
private fun PreviewConnectionItemWithName() {
  PreviewConnectionItem(
      nickName = "TEST",
      limit = null,
      totalBytes = ByteTransferReport.EMPTY,
  )
}

@Preview
@Composable
private fun PreviewConnectionItemWithLimit() {
  PreviewConnectionItem(
      nickName = "",
      limit = BandwidthLimit(10UL, BandwidthUnit.MB),
      totalBytes = ByteTransferReport.EMPTY,
  )
}

@Preview
@Composable
private fun PreviewConnectionItemUnderLimit() {
  PreviewConnectionItem(
      nickName = "TEST",
      limit = BandwidthLimit(10UL, BandwidthUnit.MB),
      totalBytes =
          ByteTransferReport(
              internetToProxy = 5UL,
              proxyToInternet = 5UL,
          ),
  )
}

@Preview
@Composable
private fun PreviewConnectionItemOverLimit() {
  PreviewConnectionItem(
      nickName = "TEST",
      limit = BandwidthLimit(5UL, BandwidthUnit.MB),
      totalBytes =
          ByteTransferReport(
              internetToProxy = (10UL * 1024UL * 1024UL),
              proxyToInternet = (10UL * 1024UL * 1024UL),
          ),
  )
}
