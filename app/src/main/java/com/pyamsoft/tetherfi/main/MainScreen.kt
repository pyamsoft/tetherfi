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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.util.rememberAsStateList
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.TestOnly

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: ServerViewState,
    pagerState: PagerState,
    allTabs: List<MainView>,

    // Settings
    onTabChanged: (MainView) -> Unit,
    onSettingsOpen: () -> Unit,

    // Actions
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onJumpToHowTo: () -> Unit,
    onLaunchIntent: (String) -> Unit,
    onShowSlowSpeedHelp: () -> Unit,

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
  ) { pv ->
    Column {
      MainTopBar(
          modifier = Modifier.fillMaxWidth(),
          appName = appName,
          pagerState = pagerState,
          allTabs = allTabs,
          onSettingsOpen = onSettingsOpen,
          onTabChanged = onTabChanged,
      )

      MainContent(
          modifier =
              Modifier.fillMaxWidth()
                  .weight(1F)
                  // So this basically doesn't do anything since we handle the padding ourselves
                  // BUT, we don't just want to consume it because we DO actually care when using
                  // Modifier.navigationBarsPadding()
                  .heightIn(
                      min = remember(pv) { pv.calculateBottomPadding() },
                  ),
          appName = appName,
          pagerState = pagerState,
          state = state,
          allTabs = allTabs,
          onShowQRCode = onShowQRCode,
          onRefreshConnection = onRefreshConnection,
          onJumpToHowTo = onJumpToHowTo,
          onUpdateTile = onUpdateTile,
          onLaunchIntent = onLaunchIntent,
          onShowSlowSpeedHelp = onShowSlowSpeedHelp,
      )
    }
  }
}

@TestOnly
@Composable
private fun PreviewMainScreen(
    isSettingsOpen: Boolean,
    isShowingQr: Boolean,
    isShowingSlowSpeedHelp: Boolean,
) {
  val state =
      object : MainViewState {
        override val isSettingsOpen = MutableStateFlow(isSettingsOpen)
        override val isShowingQRCodeDialog = MutableStateFlow(isShowingQr)
        override val isShowingSlowSpeedHelp = MutableStateFlow(isShowingSlowSpeedHelp)
        override val group = MutableStateFlow(BroadcastNetworkStatus.GroupInfo.Empty)
        override val connection = MutableStateFlow(BroadcastNetworkStatus.ConnectionInfo.Empty)
        override val port = MutableStateFlow(0)

        // TODO support RNDIS
        override val broadcastType = MutableStateFlow(BroadcastType.WIFI_DIRECT)
      }
  val allTabs = MainView.entries.rememberAsStateList()

  MainScreen(
      appName = "TEST",
      state = state,
      pagerState = rememberPagerState { allTabs.size },
      allTabs = allTabs,
      onTabChanged = {},
      onSettingsOpen = {},
      onShowQRCode = {},
      onRefreshConnection = {},
      onJumpToHowTo = {},
      onLaunchIntent = {},
      onUpdateTile = {},
      onShowSlowSpeedHelp = {},
  )
}

@Preview
@Composable
private fun PreviewMainScreenDefault() {
  PreviewMainScreen(isSettingsOpen = false, isShowingQr = false, isShowingSlowSpeedHelp = false)
}

@Preview
@Composable
private fun PreviewMainScreenSettings() {
  PreviewMainScreen(isSettingsOpen = true, isShowingQr = false, isShowingSlowSpeedHelp = false)
}

@Preview
@Composable
private fun PreviewMainScreenQr() {
  PreviewMainScreen(isSettingsOpen = false, isShowingQr = true, isShowingSlowSpeedHelp = false)
}

@Preview
@Composable
private fun PreviewMainScreenHelp() {
  PreviewMainScreen(isSettingsOpen = false, isShowingQr = true, isShowingSlowSpeedHelp = true)
}

@Preview
@Composable
private fun PreviewMainScreenAll() {
  PreviewMainScreen(isSettingsOpen = true, isShowingQr = true, isShowingSlowSpeedHelp = true)
}
