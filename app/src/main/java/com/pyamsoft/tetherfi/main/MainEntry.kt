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

import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEventEffect
import com.pyamsoft.pydroid.ui.util.fillUpToPortraitHeight
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.qr.QRCodeEntry
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.settings.SettingsDialog
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MainInjector @Inject internal constructor() : ComposableInjector() {

  @JvmField @Inject internal var viewModel: MainViewModeler? = null
  @JvmField @Inject internal var appEnvironment: AppDevEnvironment? = null

  override fun onInject(activity: ComponentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).inject(this)
  }

  override fun onDispose() {
    viewModel = null
    appEnvironment = null
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun WatchTabSwipe(
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
) {
  val hapticManager = LocalHapticManager.current

  // Watch for a swipe causing a page change and update accordingly
  LaunchedEffect(
      pagerState,
      allTabs,
      hapticManager,
  ) {
    // We don't need to remember and re-render off this, so it's fine here
    // We keep track of the previous page to avoid having the device
    // buzz when the tabs "initially mount"
    var previousPage: MainView? = null

    snapshotFlow { pagerState.targetPage }
        .distinctUntilChanged()
        .mapNotNull { allTabs.getOrNull(it) }
        .collect { page ->
          Timber.d { "Page swiped: $page" }
          // Buzz only when we are caused by a Swipe
          // since, by default, this is mounted with NULL
          if (previousPage != null) {
            withContext(context = Dispatchers.Main) { hapticManager?.actionButtonPress() }
          }
          previousPage = page
        }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MountHooks(
    viewModel: MainViewModeler,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
    onShowInAppRating: () -> Unit,
) {
  val handleShowInAppRating by rememberUpdatedState(onShowInAppRating)

  SaveStateDisposableEffect(viewModel)

  WatchTabSwipe(
      pagerState = pagerState,
      allTabs = allTabs,
  )

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }

  LaunchedEffect(viewModel) {
    viewModel.watchForInAppRatingPrompt(
        scope = this,
        onShowInAppRating = { handleShowInAppRating() },
    )
  }

  LifecycleEventEffect(
      event = Lifecycle.Event.ON_START,
  ) {
    viewModel.handleAnalyticsMarkOpened()
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainEntry(
    modifier: Modifier = Modifier,
    appName: String,

    // Action
    onShowInAppRating: () -> Unit,
    onLaunchIntent: (String) -> Unit,

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,
) {
  val handleShowInAppRating by rememberUpdatedState(onShowInAppRating)

  val scope = rememberCoroutineScope()
  val component = rememberComposableInjector { MainInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val appEnvironment = rememberNotNull(component.appEnvironment)

  val allTabs = rememberAllTabs()
  val pagerState =
      rememberPagerState(
          initialPage = 0,
          initialPageOffsetFraction = 0F,
          pageCount = { allTabs.size },
      )

  val handleTabSelected by rememberUpdatedState { tab: MainView ->
    // Click fires the index to update
    // The index updating is caught by the snapshot flow
    // Which then triggers the page update function
    val index = allTabs.indexOf(tab)
    scope.launch(context = Dispatchers.Main) { pagerState.animateScrollToPage(index) }
  }

  MountHooks(
      viewModel = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
      onShowInAppRating = { handleShowInAppRating() },
  )

  MainScreen(
      modifier = modifier,
      appName = appName,
      state = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
      onTabChanged = { handleTabSelected(it) },
      onSettingsOpen = { viewModel.handleOpenSettings() },
      onShowQRCode = { viewModel.handleOpenQRCodeDialog() },
      onRefreshConnection = { viewModel.handleRefreshConnectionInfo(scope) },
      onJumpToHowTo = { handleTabSelected(MainView.INFO) },
      onUpdateTile = onUpdateTile,
      onLaunchIntent = onLaunchIntent,
  )

  val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
  if (isSettingsOpen) {
    SettingsDialog(
        modifier =
            Modifier.fillUpToPortraitHeight()
                .widthIn(
                    max = LANDSCAPE_MAX_WIDTH,
                ),
        appEnvironment = appEnvironment,
        onDismiss = { viewModel.handleCloseSettings() },
    )
  }

  val isShowingQRCodeDialog by viewModel.isShowingQRCodeDialog.collectAsStateWithLifecycle()
  if (isShowingQRCodeDialog) {
    val group by viewModel.group.collectAsStateWithLifecycle()

    group.cast<BroadcastNetworkStatus.GroupInfo.Connected>()?.also { grp ->
      QRCodeEntry(
          ssid = grp.ssid,
          password = grp.password,
          onDismiss = { viewModel.handleCloseQRCodeDialog() },
      )
    }
  }
}
