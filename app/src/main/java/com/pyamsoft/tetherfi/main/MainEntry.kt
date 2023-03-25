package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.qr.QRCodeEntry
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.settings.SettingsDialog
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

internal class MainInjector @Inject internal constructor() : ComposableInjector() {

  @JvmField @Inject internal var viewModel: MainViewModeler? = null
  @JvmField @Inject internal var appEnvironment: AppDevEnvironment? = null

  override fun onInject(activity: FragmentActivity) {
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
  // Watch for a swipe causing a page change and update accordingly
  LaunchedEffect(
      pagerState,
      allTabs,
  ) {
    snapshotFlow { pagerState.currentPage }
        .collectLatest { index ->
          val page = allTabs[index]
          Timber.d("Page swiped: $page")
        }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MountHooks(
    component: MainInjector,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
) {
  val viewModel = rememberNotNull(component.viewModel)

  SaveStateDisposableEffect(viewModel)

  WatchTabSwipe(
      pagerState = pagerState,
      allTabs = allTabs,
  )

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }

  LifecycleEffect {
    object : DefaultLifecycleObserver {

      override fun onResume(owner: LifecycleOwner) {
        viewModel.handleRefreshConnectionInfo()
      }
    }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainEntry(
    modifier: Modifier = Modifier,
    appName: String,
) {
  val component = rememberComposableInjector { MainInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val appEnvironment = rememberNotNull(component.appEnvironment)

  val pagerState = rememberPagerState()
  val allTabs = rememberAllTabs()

  val state = viewModel.state

  MountHooks(
      component = component,
      pagerState = pagerState,
      allTabs = allTabs,
  )

  MainScreen(
      modifier = modifier,
      appName = appName,
      state = state,
      pagerState = pagerState,
      allTabs = allTabs,
      onSettingsOpen = { viewModel.handleOpenSettings() },
      onShowQRCode = { viewModel.handleOpenQRCodeDialog() },
      onRefreshGroup = { viewModel.handleRefreshConnectionInfo() },
      onRefreshConnection = { viewModel.handleRefreshConnectionInfo() },
  )

  val isSettingsOpen by state.isSettingsOpen.collectAsState()
  if (isSettingsOpen) {
    SettingsDialog(
        appEnvironment = appEnvironment,
        onDismiss = { viewModel.handleCloseSettings() },
    )
  }

  val isShowingQRCodeDialog by state.isShowingQRCodeDialog.collectAsState()
  if (isShowingQRCodeDialog) {
    val group by state.group.collectAsState()

    (group as? WiDiNetworkStatus.GroupInfo.Connected)?.also { grp ->
      QRCodeEntry(
          ssid = grp.ssid,
          password = grp.password,
          onDismiss = { viewModel.handleCloseQRCodeDialog() },
      )
    }
  }
}
