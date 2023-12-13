/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status.sections

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.status.StatusScreenContentTypes
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard
import java.util.concurrent.Executors

internal fun LazyListScope.renderBattery(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    tileServiceClass: Class<out TileService>,
    appName: String,
    @DrawableRes appIcon: Int,
    state: StatusViewState,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,
) {
  item(
      contentType = StatusScreenContentTypes.BATTERY_LABEL,
  ) {
    Label(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Operating Settings",
    )
  }

  item(
      contentType = StatusScreenContentTypes.BATTERY_OPTIMIZATION,
  ) {
    BatteryOptimization(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        state = state,
        onDisableBatteryOptimizations = onDisableBatteryOptimizations,
    )
  }

  item(
      contentType = StatusScreenContentTypes.BATTERY_ADD_TILE,
  ) {
    AddTheTile(
        modifier = itemModifier,
        isEditable = isEditable,
        tileServiceClass = tileServiceClass,
        appName = appName,
        appIcon = appIcon,
    )
  }
}

@Composable
@CheckResult
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun rememberTileClickHandler(
    tileServiceClass: Class<out TileService>,
    appName: String,
    @DrawableRes appIcon: Int,
): State<() -> Unit> {
  val context = LocalContext.current
  val appContext = remember(context) { context.applicationContext }
  val statusBarManager =
      remember(appContext) { appContext.getSystemService<StatusBarManager>().requireNotNull() }
  val componentName =
      remember(appContext) {
        ComponentName(
            appContext.packageName,
            tileServiceClass.name,
        )
      }
  val icon = remember(appContext) { Icon.createWithResource(context, appIcon) }
  return rememberUpdatedState {
    Timber.d { "Add Tile: $componentName" }
    statusBarManager.requestAddTileService(
        componentName,
        appName,
        icon,
        Executors.newSingleThreadExecutor(),
    ) { code ->
      Timber.d { "Tile Add Result Code: $code" }
    }
  }
}

@Composable
private fun AddTheTile(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    tileServiceClass: Class<out TileService>,
    appName: String,
    @DrawableRes appIcon: Int,
) {
  val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color = MaterialTheme.colors.primary.copy(alpha = mediumAlpha),
              shape = MaterialTheme.shapes.medium,
          ),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hapticManager = LocalHapticManager.current
        val handleClick by
            rememberTileClickHandler(
                tileServiceClass = tileServiceClass,
                appName = appName,
                appIcon = appIcon,
            )

        OutlinedButton(
            onClick = {
              hapticManager?.confirmButtonPress()
              handleClick()
            },
        ) {
          Text(
              text = "Add the Tile",
          )
        }
      }
    }
  }
}

@Composable
private fun BatteryOptimization(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    onDisableBatteryOptimizations: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val isBatteryOptimizationDisabled by
      state.isBatteryOptimizationsIgnored.collectAsStateWithLifecycle()

  val canEdit =
      remember(isEditable, isBatteryOptimizationDisabled) {
        isEditable && !isBatteryOptimizationDisabled
      }

  CheckableCard(
      modifier = modifier,
      isEditable = canEdit,
      condition = isBatteryOptimizationDisabled,
      title = "Always Alive",
      description =
          """This will allow the $appName Hotspot to continue running even if the app is closed.
            |
            |This will significantly enhance your experience and network performance.
            |(recommended)"""
              .trimMargin(),
      onClick = {
        if (!isBatteryOptimizationDisabled) {
          hapticManager?.confirmButtonPress()
          onDisableBatteryOptimizations()
        }
      },
  )
}
