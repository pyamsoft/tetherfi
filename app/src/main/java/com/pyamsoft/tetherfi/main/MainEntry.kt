package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.pyamsoft.pydroid.ui.util.rememberAsStateList
import com.pyamsoft.tetherfi.settings.SettingsDialog
import com.pyamsoft.tetherfi.ui.SafeList
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@Composable
@OptIn(ExperimentalPagerApi::class)
private fun WatchTabSwipe(
    pagerState: PagerState,
    allTabs: SafeList<MainView>,
) {
  // Watch for a swipe causing a page change and update accordingly
  val list = allTabs.list.rememberAsStateList()
  LaunchedEffect(
      pagerState,
      list,
  ) {
    snapshotFlow { pagerState.currentPage }
        .collectLatest { index ->
          val page = list[index]
          Timber.d("Page swiped: $page")
        }
  }
}

@Composable
@OptIn(ExperimentalPagerApi::class)
private fun MountHooks(
    pagerState: PagerState,
    allTabs: SafeList<MainView>,
) {
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
  val pagerState = rememberPagerState()
  val allTabs = rememberAllTabs()

  val (showDialog, setShowDialog) = rememberSaveable { mutableStateOf(false) }
  val handleDismissSettings by rememberUpdatedState { setShowDialog(false) }
  val handleShowSettings by rememberUpdatedState { setShowDialog(true) }

  MountHooks(
      pagerState = pagerState,
      allTabs = allTabs,
  )

  MainScreen(
      modifier = modifier,
      appName = appName,
      pagerState = pagerState,
      allTabs = allTabs,
      onSettingsOpen = handleShowSettings,
  )

  if (showDialog) {
    SettingsDialog(
        onDismiss = handleDismissSettings,
    )
  }
}
