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

package com.pyamsoft.tetherfi.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.PYDroidActivityDelegate
import com.pyamsoft.pydroid.ui.app.installPYDroid
import com.pyamsoft.pydroid.ui.changelog.ChangeLogProvider
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.util.fillUpToPortraitSize
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.getSystemDarkMode
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.status.tweaks.ScreenOnHandler
import com.pyamsoft.tetherfi.tile.ProxyTileService
import com.pyamsoft.tetherfi.ui.InstallPYDroidExtras
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: ThemeViewModeler? = null
  @Inject @JvmField internal var launcher: ServiceLauncher? = null
  @Inject @JvmField internal var screenOnHandler: ScreenOnHandler? = null

  private var pydroid: PYDroidActivityDelegate? = null

  private fun initializePYDroid() {
    pydroid =
        installPYDroid(
            provider =
                object : ChangeLogProvider {

                  override val applicationIcon = R.mipmap.ic_launcher

                  override val changelog = buildChangeLog {
                    bugfix("When stopping the hotspot, stop operation happens faster.")
                    change(
                        "Wake Locks have been made ON by default. The option to toggle them will be removed in the next version.")
                    change(
                        "The \"Enable Idle Timeout\" tweak will be made a default in the next version")
                    feature("New Shortcut to directly start the hotspot")
                  }
                },
        )
  }

  private fun handleShowInAppRating() {
    pydroid?.loadInAppRating()
  }

  private fun setupActivity() {
    // Setup PYDroid first
    initializePYDroid()

    // Create and initialize the ObjectGraph
    val component = ObjectGraph.ApplicationScope.retrieve(this).plusMain().create()
    component.inject(this)
    ObjectGraph.ActivityScope.install(this, component)

    // Then register for any permissions
    PermissionManager.createAndRegister(this, component)

    // Watch the hotspot status and keep the screen on if we are allowed
    screenOnHandler.requireNotNull().bind(this)
  }

  private fun safeOpenSettingsIntent(action: String) {
    // Try specific first, may fail on some devices
    try {
      val intent = Intent(action, "package:${packageName}".toUri())
      startActivity(intent)
    } catch (e: Throwable) {
      Timber.e(e) { "Failed specific intent for $action" }
      val intent = Intent(action)
      startActivity(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupActivity()

    val vm = viewModel.requireNotNull()
    val appName = getString(R.string.app_name)

    setContent {
      val theme by vm.mode.collectAsStateWithLifecycle()
      val isMaterialYou by vm.isMaterialYou.collectAsStateWithLifecycle()

      SaveStateDisposableEffect(vm)

      TetherFiTheme(
          theme = theme,
          isMaterialYou = isMaterialYou,
      ) {
        SystemBars(
            isDarkMode = theme.getSystemDarkMode(),
        )
        InstallPYDroidExtras(
            modifier = Modifier.fillUpToPortraitSize().widthIn(max = LANDSCAPE_MAX_WIDTH),
            appName = appName,
        )
        MainEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            onShowInAppRating = { handleShowInAppRating() },
            onUpdateTile = { ProxyTileService.updateTile(this) },
            onLaunchIntent = { safeOpenSettingsIntent(it) },
        )
      }
    }

    vm.init(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    reportFullyDrawn()
  }

  override fun onDestroy() {
    super.onDestroy()
    pydroid = null
    viewModel = null
    launcher = null
    screenOnHandler = null
  }
}
