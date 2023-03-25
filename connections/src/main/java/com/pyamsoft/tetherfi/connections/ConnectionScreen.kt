package com.pyamsoft.tetherfi.connections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.util.collectAsStateList
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.clients.key
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    state: ConnectionViewState,
    serverViewState: ServerViewState,
    onToggleBlock: (TetherClient) -> Unit,
) {
  val group by serverViewState.group.collectAsState()
  val clients = state.connections.collectAsStateList()
  val blocked = state.blocked.collectAsStateList()

  Scaffold(
      modifier = modifier,
  ) { pv ->
    LazyColumn {
      item {
        Spacer(
            modifier = Modifier.statusBarsPadding().padding(pv),
        )
      }

      group.also { gi ->
        if (gi is WiDiNetworkStatus.GroupInfo.Connected) {
          if (clients.isEmpty()) {
            item {
              Text(
                  modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
                  text = "No connections yet!",
                  style = MaterialTheme.typography.h5,
                  textAlign = TextAlign.Center,
              )
            }
          } else {
            items(
                items = clients,
                key = { it.key() },
            ) { client ->
              ConnectionItem(
                  modifier = Modifier.fillMaxWidth(),
                  client = client,
                  blocked = blocked,
                  onClick = onToggleBlock,
              )
            }
          }
        } else {
          item {
            Text(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
                text = "Start the Hotspot to view and manage connected devices.",
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center,
            )
          }

          item {
            Text(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
                text =
                    "The system isn't perfect. Android does not let me see which device is which, so this screen can only allow you to manage things by IP address.",
                style =
                    MaterialTheme.typography.body2.copy(
                        color =
                            MaterialTheme.colors.onBackground.copy(
                                alpha = ContentAlpha.disabled,
                            ),
                    ),
                textAlign = TextAlign.Center,
            )
          }

          item {
            Text(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
                text = "A more friendly solution is being actively investigated.",
                style =
                    MaterialTheme.typography.body2.copy(
                        color =
                            MaterialTheme.colors.onBackground.copy(
                                alpha = ContentAlpha.disabled,
                            ),
                    ),
                textAlign = TextAlign.Center,
            )
          }
        }
      }

      item {
        Spacer(
            modifier = Modifier.navigationBarsPadding().padding(pv),
        )
      }
    }
  }
}

private val FIRST_SEEN_DATE_FORMATTER =
    object : ThreadLocal<DateTimeFormatter>() {

      override fun initialValue(): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
      }
    }

@Composable
private fun ConnectionItem(
    modifier: Modifier = Modifier,
    blocked: SnapshotStateList<TetherClient>,
    client: TetherClient,
    onClick: (TetherClient) -> Unit,
) {
  val name = remember(client) { client.key() }
  val seenTime =
      remember(client) { FIRST_SEEN_DATE_FORMATTER.get().requireNotNull().format(client.firstSeen) }

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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.Elevation,
        shape = MaterialTheme.shapes.medium,
    ) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              modifier = Modifier.weight(1F),
              text = name,
              style = MaterialTheme.typography.h6,
          )
          Switch(
              checked = isNotBlocked,
              onCheckedChange = { onClick(client) },
          )
        }

        Text(
            text = "First seen: $seenTime",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }
}
