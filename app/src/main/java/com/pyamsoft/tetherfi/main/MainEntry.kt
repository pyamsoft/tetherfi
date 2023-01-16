package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberActivity
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.settings.SettingsDialog
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

internal class MainInjector : ComposableInjector() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ApplicationScope.retrieve(activity).plusMain().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

@Composable
@OptIn(ExperimentalPagerApi::class)
private fun WatchTabSwipe(
    pagerState: PagerState,
    allTabs: List<MainView>,
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
    allTabs: List<MainView>,
) {
  val activity = rememberActivity()

  LaunchedEffect(
      activity,
      viewModel,
  ) {
    viewModel.handleSyncDarkTheme(activity)
  }

  WatchTabSwipe(
      pagerState = pagerState,
      allTabs = allTabs,
  )
}

@Composable
@OptIn(ExperimentalPagerApi::class)
fun MainEntry(
    modifier: Modifier = Modifier,
    appName: String,
) {
  val component = rememberComposableInjector { MainInjector() }
  val viewModel = requireNotNull(component.viewModel)

  val state = viewModel.state()

  val pagerState = rememberPagerState()
  val allTabs = rememberAllTabs()
  val activity = rememberActivity()

  val handleSettingsOpen by rememberUpdatedState { SettingsDialog.show(activity) }

  MountHooks(
      viewModel = viewModel,
      pagerState = pagerState,
      allTabs = allTabs,
  )

  activity.TetherFiTheme(
      theme = state.theme,
  ) {
    MainScreen(
        modifier = modifier,
        appName = appName,
        pagerState = pagerState,
        allTabs = allTabs,
        onSettingsOpen = handleSettingsOpen,
    )
  }
}
