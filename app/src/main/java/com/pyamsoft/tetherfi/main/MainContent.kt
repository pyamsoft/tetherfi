package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.pyamsoft.pydroid.ui.util.rememberAsStateList
import com.pyamsoft.tetherfi.info.InfoEntry
import com.pyamsoft.tetherfi.status.StatusEntry
import com.pyamsoft.tetherfi.ui.SafeList

@Composable
@OptIn(ExperimentalPagerApi::class)
fun MainContent(
    modifier: Modifier = Modifier,
    appName: String,
    pagerState: PagerState,
    allTabs: SafeList<MainView>,
) {
  val list = allTabs.list.rememberAsStateList()
  HorizontalPager(
      modifier = modifier,
      count = list.size,
      state = pagerState,
  ) { page ->
    val screen =
        remember(
            list,
            page,
        ) {
          list[page]
        }

    when (screen) {
      is MainView.Info -> {
        InfoEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
        )
      }
      is MainView.Status -> {
        StatusEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
        )
      }
    }
  }
}
