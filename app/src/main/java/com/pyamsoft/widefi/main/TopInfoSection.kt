package com.pyamsoft.widefi.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.server.status.RunningStatus

@Composable
internal fun TopInfoSection(
    modifier: Modifier = Modifier,
    state: MainViewState,
    onToggle: () -> Unit,
    onTabUpdated: (MainView) -> Unit,
) {
  val wiDiStatus = state.wiDiStatus
  val currentTab = state.view

  val tabs = remember { MainView.values() }
  val index =
      remember(tabs, currentTab) {
        val idx = tabs.indexOfFirst { it == currentTab }
        return@remember if (idx < 0) 0 else idx
      }

  val isButtonEnabled =
      remember(wiDiStatus) {
        wiDiStatus == RunningStatus.Running || wiDiStatus == RunningStatus.NotRunning
      }
  val buttonText =
      remember(wiDiStatus) {
        when (wiDiStatus) {
          is RunningStatus.Error -> "WideFi Error"
          is RunningStatus.NotRunning -> "Turn WideFi ON"
          is RunningStatus.Running -> "Turn WideFi OFF"
          else -> "WideFi is thinking..."
        }
      }

  Column(
      modifier = modifier,
  ) {
    Button(
        enabled = isButtonEnabled,
        onClick = onToggle,
    ) {
      Text(
          text = buttonText,
      )
    }

    TabRow(
        selectedTabIndex = index,
    ) {
      tabs.forEach { tab ->
        TickerTab(
            current = currentTab,
            tab = tab,
            onTabUpdated = onTabUpdated,
        )
      }
    }
    Box(
        modifier = Modifier.padding(MaterialTheme.keylines.content),
    ) {
      when (currentTab) {
        MainView.STATUS -> {
          StatusScreen(
              state = state,
          )
        }
        MainView.ACTIVITY -> {
          ActivityScreen(
              state = state,
          )
        }
      }
    }
  }
}

@Composable
private fun TickerTab(
    modifier: Modifier = Modifier,
    tab: MainView,
    current: MainView,
    onTabUpdated: (MainView) -> Unit,
) {
  Tab(
      modifier = modifier,
      selected = tab == current,
      onClick = { onTabUpdated(tab) },
  ) {
    Text(
        modifier = Modifier.padding(vertical = MaterialTheme.keylines.typography),
        text = tab.display,
    )
  }
}
