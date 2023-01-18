package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState

@Composable
@OptIn(ExperimentalPagerApi::class)
fun MainScreen(
    modifier: Modifier = Modifier,
    appName: String,
    pagerState: PagerState,
    allTabs: SnapshotStateList<MainView>,
    onSettingsOpen: () -> Unit,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
  ) { pv ->
    Column {
      MainTopBar(
          // Z-Index to place it above the SwipeRefresh indicator
          modifier = Modifier.fillMaxWidth().zIndex(1F),
          appName = appName,
          pagerState = pagerState,
          allTabs = allTabs,
          onSettingsOpen = onSettingsOpen,
      )

      MainContent(
          modifier = Modifier.fillMaxWidth().weight(1F).padding(pv),
          appName = appName,
          pagerState = pagerState,
          allTabs = allTabs,
      )
    }
  }
}
