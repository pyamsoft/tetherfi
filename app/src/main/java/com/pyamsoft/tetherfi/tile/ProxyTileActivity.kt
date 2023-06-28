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

package com.pyamsoft.tetherfi.tile

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.main.SystemBars
import com.pyamsoft.tetherfi.main.ThemeViewModeler
import javax.inject.Inject

class ProxyTileActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: ThemeViewModeler? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_TetherFi_Tile)
    super.onCreate(savedInstanceState)

    ObjectGraph.ApplicationScope.retrieve(this).plusTile().create().inject(this)
    stableLayoutHideNavigation()

    val vm = viewModel.requireNotNull()

    setContent {
      val theme by vm.theme.collectAsState()

      SystemBars()

      TetherFiTheme(
          theme = theme,
      ) {
        ProxyTileEntry(
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.requireNotNull().handleSyncDarkTheme(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    viewModel.requireNotNull().handleSyncDarkTheme(newConfig)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel = null
  }
}
