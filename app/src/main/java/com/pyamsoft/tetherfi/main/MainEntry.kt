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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.haptics.HapticManager
import com.pyamsoft.pydroid.ui.haptics.rememberHapticManager
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.fullScreenDialog
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.qr.QRCodeEntry
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.settings.SettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

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
    hapticManager: HapticManager,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
) {
  // Watch for a swipe causing a page change and update accordingly
  LaunchedEffect(
      pagerState,
      allTabs,
      hapticManager,
  ) {
    snapshotFlow { pagerState.targetPage }
        .distinctUntilChanged()
        .mapNotNull { allTabs.getOrNull(it) }
        .collect { page ->
          Timber.d("Page swiped: $page")
          withContext(context = Dispatchers.Main) { hapticManager.actionButtonPress() }
        }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MountHooks(
    hapticManager: HapticManager,
    viewModel: MainViewModeler,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
    onShowInAppRating: () -> Unit,
) {
  val handleShowInAppRating by rememberUpdatedState(onShowInAppRating)

  SaveStateDisposableEffect(viewModel)

  WatchTabSwipe(
      hapticManager = hapticManager,
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

  LifecycleEffect {
    object : DefaultLifecycleObserver {

      override fun onResume(owner: LifecycleOwner) {
        viewModel.handleRefreshConnectionInfo(owner.lifecycleScope)
      }

      override fun onStart(owner: LifecycleOwner) {
        viewModel.handleAnalyticsMarkOpened()
      }
    }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainEntry(
    modifier: Modifier = Modifier,
    appName: String,
    onShowInAppRating: () -> Unit,
) {
  val handleShowInAppRating by rememberUpdatedState(onShowInAppRating)

  val scope = rememberCoroutineScope()
  val component = rememberComposableInjector { MainInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val appEnvironment = rememberNotNull(component.appEnvironment)

  val pagerState = rememberPagerState()
  val allTabs = rememberAllTabs()
  val hapticManager = rememberHapticManager()

  MountHooks(
      hapticManager = hapticManager,
      viewModel = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
      onShowInAppRating = { handleShowInAppRating() },
  )

  MainScreen(
      modifier = modifier,
      hapticManager = hapticManager,
      appName = appName,
      state = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
      onSettingsOpen = { viewModel.handleOpenSettings() },
      onShowQRCode = { viewModel.handleOpenQRCodeDialog() },
      onRefreshConnection = { viewModel.handleRefreshConnectionInfo(scope) },
  )

  val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
  if (isSettingsOpen) {
    SettingsDialog(
        modifier = Modifier.fullScreenDialog(),
        appEnvironment = appEnvironment,
        onDismiss = { viewModel.handleCloseSettings() },
    )
  }

  val isShowingQRCodeDialog by viewModel.isShowingQRCodeDialog.collectAsState()
  if (isShowingQRCodeDialog) {
    val group by viewModel.group.collectAsState()

    (group as? WiDiNetworkStatus.GroupInfo.Connected)?.also { grp ->
      QRCodeEntry(
          ssid = grp.ssid,
          password = grp.password,
          onDismiss = { viewModel.handleCloseQRCodeDialog() },
      )
    }
  }
}
