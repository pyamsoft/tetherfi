/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status.sections.operating

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.tile.TileStatus
import java.util.concurrent.Executors

@Composable
@CheckResult
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun rememberTileClickHandler(
    status: TileStatus,
    tileServiceClass: Class<out TileService>,
    appName: String,
    @DrawableRes appIcon: Int,
): State<() -> Unit> {
  val context = LocalContext.current
  val appContext = remember(context) { context.applicationContext }
  val executor = remember { Executors.newSingleThreadExecutor() }
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
    statusBarManager.requestAddTileService(
        componentName,
        appName,
        icon,
        executor,
    ) { code ->
      when (code) {
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
          Timber.d { "Tile is added/was already added, mark alive $code" }
          status.markAlive()
        }
        else -> {
          Timber.w { "Tile added error, mark dead $code" }
          status.markDead()
        }
      }
    }
  }
}

@Composable
internal fun QuickTileAddButton(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
) {
  // If we can, show the quick button
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val component = rememberComposableInjector { QuickTileAddButtonInjector() }
    val hapticManager = LocalHapticManager.current

    val status = rememberNotNull(component.tileStatus)
    val appIconRes = rememberNotNull(component.appIconForegroundRes)

    val isTileAdded by status.status().collectAsStateWithLifecycle()

    val isButtonEnabled =
        remember(
            isEditable,
            isTileAdded,
        ) {
          if (isEditable) {
            // IF tile is not already added, button is on
            return@remember !isTileAdded
          } else {
            return@remember false
          }
        }

    val handleClick by
        rememberTileClickHandler(
            status = status,
            tileServiceClass = rememberNotNull(component.tileServiceClass),
            appName = appName,
            appIcon = appIconRes,
        )

    Button(
        modifier = modifier,
        enabled = isButtonEnabled,
        onClick = {
          hapticManager?.confirmButtonPress()
          handleClick()
        },
    ) {
      Text(
          text = "Add Quick Settings Tile",
      )
    }
  }
}
