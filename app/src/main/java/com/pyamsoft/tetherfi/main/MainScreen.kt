/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.ui.haptics.HapticManager
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainScreen(
    modifier: Modifier = Modifier,
    hapticManager: HapticManager,
    appName: String,
    state: ServerViewState,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,

    // Settings
    onSettingsOpen: () -> Unit,

    // Running status
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
  ) { pv ->
    Column {
      MainTopBar(
          modifier = Modifier.fillMaxWidth(),
          hapticManager = hapticManager,
          appName = appName,
          pagerState = pagerState,
          allTabs = allTabs,
          onSettingsOpen = onSettingsOpen,
      )

      MainContent(
          modifier = Modifier.fillMaxWidth().weight(1F).padding(pv),
          appName = appName,
          pagerState = pagerState,
          state = state,
          allTabs = allTabs,
          onShowQRCode = onShowQRCode,
          onRefreshConnection = onRefreshConnection,
      )
    }
  }
}
