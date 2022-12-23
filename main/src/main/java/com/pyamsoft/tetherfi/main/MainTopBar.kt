package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.theme.ZeroElevation

private val ALL_TABS =
    listOf(
        MainView.Status,
        MainView.Info,
    )

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    appName: String,
    selected: MainView,
    onSettingsOpen: () -> Unit,
    onTabSelected: (MainView) -> Unit,
) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colors.background,
      elevation = ZeroElevation,
  ) {
    val selectedIndex = remember(selected) { ALL_TABS.indexOf(selected) }

    Surface(
        contentColor = MaterialTheme.colors.onPrimary,
        color = MaterialTheme.colors.primary,
        shape =
            MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize,
            ),
        elevation = AppBarDefaults.TopAppBarElevation,
    ) {
      Column {
        TopAppBar(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            elevation = ZeroElevation,
            backgroundColor = Color.Transparent,
            contentColor = LocalContentColor.current,
            title = {
              Text(
                  text = appName,
              )
            },
            actions = {
              IconButton(
                  onClick = onSettingsOpen,
              ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open Settings",
                )
              }
            },
        )

        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = selectedIndex,
            backgroundColor = Color.Transparent,
            contentColor = LocalContentColor.current,
        ) {
          ALL_TABS.forEach { tab ->
            val isSelected =
                remember(
                    tab,
                    selected,
                ) {
                  tab == selected
                }

            val textStyle = LocalTextStyle.current
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                  Text(
                      text = tab.name,
                      style =
                          textStyle.copy(
                              fontWeight = if (isSelected) FontWeight.W700 else null,
                          ),
                  )
                },
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun PreviewMainTopBar() {
  MainTopBar(
      appName = "TEST",
      selected = MainView.Status,
      onSettingsOpen = {},
      onTabSelected = {},
  )
}
