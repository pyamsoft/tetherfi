package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.qr.QRCodeEntry
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.settings.SettingsDialog
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

internal class MainInjector @Inject internal constructor() : ComposableInjector() {

  @JvmField @Inject internal var viewModel: MainViewModeler? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

@Composable
@OptIn(ExperimentalPagerApi::class)
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
@OptIn(ExperimentalPagerApi::class)
private fun MountHooks(
    viewModel: MainViewModeler,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
) {
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
@OptIn(ExperimentalPagerApi::class)
fun MainEntry(
    modifier: Modifier = Modifier,
    appName: String,
) {
  val component = rememberComposableInjector { MainInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  val pagerState = rememberPagerState()
  val allTabs = rememberAllTabs()

  val state = viewModel.state

  MountHooks(
      viewModel = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
  )
  SaveStateDisposableEffect(viewModel)

  MainScreen(
      modifier = modifier,
      appName = appName,
      state = state,
      pagerState = pagerState,
      allTabs = allTabs,
      onSettingsOpen = { viewModel.handleOpenSettings() },
      onShowQRCode = { viewModel.handleOpenQRCodeDialog() },
  )

  val isSettingsOpen by state.isSettingsOpen.collectAsState()
  if (isSettingsOpen) {
    SettingsDialog(
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
