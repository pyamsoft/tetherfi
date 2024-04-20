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

package com.pyamsoft.tetherfi.status.sections.performance

import androidx.annotation.CheckResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.status.StatusViewState

private enum class PowerBalanceDialogContentTypes {
  DIALOG_LIMITS,
  DIALOG_ACTIONS
}

private data class DisplayLimit(
    val key: String,
    val limit: ServerPerformanceLimit,
    val title: String,
    val description: String,
)

@Composable
@CheckResult
private fun rememberDisplayLimits(): List<DisplayLimit> {
  return remember {
    return@remember ServerPerformanceLimit.Defaults.entries
        .map { l ->
          val title: String
          val description: String
          val t = l.coroutineLimit
          when (l) {
            ServerPerformanceLimit.Defaults.UNBOUND -> {
              title = "Max Performance"
              description =
                  "Unlimited threads. Offers maximum performance. Throws battery usage concerns out the window."
            }
            ServerPerformanceLimit.Defaults.BOUND_N_CPU -> {
              title = "Max Battery-Saving"
              description =
                  "$t threads. Uses the least battery but offers the slowest hotspot performance."
            }
            ServerPerformanceLimit.Defaults.BOUND_2N_CPU -> {
              title = "Battery-Saving"
              description = "$t threads. Uses less battery but offers slower hotspot performance."
            }
            ServerPerformanceLimit.Defaults.BOUND_3N_CPU -> {
              title = "Balanced"
              description =
                  "$t threads. Offers a balance between battery usage and hotspot performance."
            }
            ServerPerformanceLimit.Defaults.BOUND_4N_CPU -> {
              title = "Performance"
              description =
                  "$t threads. Offers better hotspot performance at the cost of more battery usage."
            }
            ServerPerformanceLimit.Defaults.BOUND_5N_CPU -> {
              title = "High Performance"
              description =
                  "$t threads. Offers great hotspot performance at the cost of significant battery usage."
            }
          }

          return@map DisplayLimit(
              key = l.name,
              limit = l,
              title = title,
              description = description,
          )
        }
        .sortedWith { o1, o2 ->
          // Unbound on bottom
          if (o1.limit.coroutineLimit <= 0) {
            return@sortedWith 1
          }

          // Unbound on bottom
          if (o2.limit.coroutineLimit <= 0) {
            return@sortedWith -1
          }

          return@sortedWith o1.limit.coroutineLimit - o2.limit.coroutineLimit
        }
  }
}

@Composable
private fun DialogClose(
    modifier: Modifier = Modifier,
    onHidePowerBalance: () -> Unit,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(
        modifier = Modifier.weight(1F),
    )
    TextButton(
        onClick = onHidePowerBalance,
    ) {
      Text(
          text = "Close",
      )
    }
  }
}

@Composable
private fun PowerBalanceLimit(
    modifier: Modifier = Modifier,
    current: ServerPerformanceLimit,
    limit: ServerPerformanceLimit,
    title: String,
    description: String,
    onSelect: (ServerPerformanceLimit) -> Unit,
) {
  val isMax = remember(limit) { limit.coroutineLimit <= 0 }
  val isCurrent =
      remember(
          current,
          limit,
      ) {
        current.coroutineLimit == limit.coroutineLimit
      }

  Row(
      modifier =
          modifier
              .padding(top = MaterialTheme.keylines.content * if (isMax) 2 else 1)
              .clickable(
                  enabled = !isCurrent,
              ) {
                onSelect(limit)
              }
              .padding(horizontal = MaterialTheme.keylines.content),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(
        modifier = Modifier.padding(end = MaterialTheme.keylines.baseline),
        selected = isCurrent,
        onClick = null,
    )

    Column(
        modifier = Modifier.weight(1F),
    ) {
      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.typography),
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.W700,
      )

      Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun PowerBalanceDialog(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onHidePowerBalance: () -> Unit,
    onUpdatePowerBalance: (ServerPerformanceLimit) -> Unit,
) {
  val currentLimit by state.powerBalance.collectAsStateWithLifecycle()
  val limits = rememberDisplayLimits()

  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onHidePowerBalance,
  ) {
    Card(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
        shape = MaterialTheme.shapes.medium,
    ) {
      LazyColumn {
        items(
            items = limits,
            key = { it.key },
            contentType = { PowerBalanceDialogContentTypes.DIALOG_LIMITS },
        ) { limit ->
          PowerBalanceLimit(
              modifier = Modifier.fillMaxWidth(),
              current = currentLimit,
              limit = limit.limit,
              title = limit.title,
              description = limit.description,
              onSelect = {
                onUpdatePowerBalance(it)
                onHidePowerBalance()
              },
          )
        }

        item(
            contentType = PowerBalanceDialogContentTypes.DIALOG_ACTIONS,
        ) {
          DialogClose(
              modifier =
                  Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.keylines.content),
              onHidePowerBalance = onHidePowerBalance,
          )
        }
      }
    }
  }
}
