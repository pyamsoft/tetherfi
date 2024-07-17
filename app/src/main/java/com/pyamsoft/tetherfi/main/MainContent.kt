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

package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.tetherfi.connections.ConnectionEntry
import com.pyamsoft.tetherfi.info.InfoEntry
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.StatusEntry
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainContent(
    modifier: Modifier = Modifier,
    appName: String,
    pagerState: PagerState,
    state: ServerViewState,
    allTabs: List<MainView>,

    // Actions
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onJumpToHowTo: () -> Unit,
    onLaunchIntent: (String) -> Unit,
    onShowSlowSpeedHelp: () -> Unit,

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,
) {
  HorizontalPager(
      modifier = modifier,
      state = pagerState,
  ) { page ->
    val screen =
        remember(
            allTabs,
            page,
        ) {
          allTabs[page]
        }

    when (screen) {
      MainView.INFO -> {
        InfoEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            serverViewState = state,
            onShowQRCode = onShowQRCode,
            onShowSlowSpeedHelp = onShowSlowSpeedHelp,
        )
      }
      MainView.STATUS -> {
        StatusEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            serverViewState = state,
            onShowQRCode = onShowQRCode,
            onRefreshConnection = onRefreshConnection,
            onJumpToHowTo = onJumpToHowTo,
            onUpdateTile = onUpdateTile,
            onLaunchIntent = onLaunchIntent,
            onShowSlowSpeedHelp = onShowSlowSpeedHelp,
        )
      }
      MainView.CONNECTIONS -> {
        ConnectionEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            serverViewState = state,
        )
      }
    }
  }
}
