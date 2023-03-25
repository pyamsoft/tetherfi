package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.pyamsoft.tetherfi.connections.ConnectionEntry
import com.pyamsoft.tetherfi.info.InfoEntry
import com.pyamsoft.tetherfi.status.StatusEntry
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MainContent(
    modifier: Modifier = Modifier,
    appName: String,
    pagerState: PagerState,
    state: ServerViewState,
    allTabs: SnapshotStateList<MainView>,
    onShowQRCode: () -> Unit,
    onRefreshGroup: () -> Unit,
    onRefreshConnection: () -> Unit,
) {
  HorizontalPager(
      modifier = modifier,
      pageCount = allTabs.size,
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
        )
      }
      MainView.STATUS -> {
        StatusEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            serverViewState = state,
            onShowQRCode = onShowQRCode,
            onRefreshGroup = onRefreshGroup,
            onRefreshConnection = onRefreshConnection,
        )
      }
      MainView.CONNECTIONS -> {
        ConnectionEntry(
            modifier = Modifier.fillMaxSize(),
            serverViewState = state,
        )
      }
    }
  }
}
