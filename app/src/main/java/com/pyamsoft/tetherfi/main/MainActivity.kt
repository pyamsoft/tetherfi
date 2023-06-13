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

package com.pyamsoft.tetherfi.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.PYDroidActivityDelegate
import com.pyamsoft.pydroid.ui.app.installPYDroid
import com.pyamsoft.pydroid.ui.changelog.ChangeLogProvider
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.util.doOnCreate
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.ui.InstallPYDroidExtras
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: ThemeViewModeler? = null
  @Inject @JvmField internal var permissions: MainPermissions? = null
  private var pydroid: PYDroidActivityDelegate? = null

  init {
    doOnCreate {
      pydroid =
          installPYDroid(
              provider =
                  object : ChangeLogProvider {

                    override val applicationIcon = R.mipmap.ic_launcher

                    override val changelog = buildChangeLog {
                      bugfix("Reduce memory usage and make sure we close all sockets after use")
                      change("Correct attribution for open source libraries")
                      change("Remove unused code to shrink install size")
                      change(
                          "Clear old clients out of Connections if they haven't had activity for 3 minutes.")
                    }
                  },
          )
    }

    doOnCreate { permissions.requireNotNull().register(this) }
  }

  private fun handleShowInAppRating() {
    pydroid?.loadInAppRating()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    stableLayoutHideNavigation()

    val component = ObjectGraph.ApplicationScope.retrieve(this).plusMain().create()
    component.inject(this)
    ObjectGraph.ActivityScope.install(this, component)

    val vm = viewModel.requireNotNull()
    val appName = getString(R.string.app_name)

    setContent {
      val state = vm.state
      val theme by state.theme.collectAsState()

      TetherFiTheme(
          theme = theme,
      ) {
        SystemBars()
        InstallPYDroidExtras()
        MainEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
            onShowInAppRating = { handleShowInAppRating() },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    viewModel.requireNotNull().handleSyncDarkTheme(this)
    reportFullyDrawn()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    viewModel?.handleSyncDarkTheme(newConfig)
  }

  override fun onDestroy() {
    super.onDestroy()

    permissions?.unregister()

    pydroid = null
    permissions = null
    viewModel = null
  }
}
