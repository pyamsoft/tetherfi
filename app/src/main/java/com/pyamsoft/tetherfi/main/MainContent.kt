package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.pyamsoft.tetherfi.info.InfoEntry
import com.pyamsoft.tetherfi.status.StatusEntry
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
@OptIn(ExperimentalPagerApi::class)
fun MainContent(
    modifier: Modifier = Modifier,
    appName: String,
    pagerState: PagerState,
    state: ServerViewState,
    allTabs: SnapshotStateList<MainView>,
    onShowQRCode: () -> Unit,
) {
  HorizontalPager(
      modifier = modifier,
      count = allTabs.size,
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
      is MainView.Info -> {
        InfoEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            serverViewState = state,
        )
      }
      is MainView.Status -> {
        StatusEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            onShowQRCode = onShowQRCode,
            serverViewState = state,
        )
      }
    }
  }
}
