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
import com.pyamsoft.tetherfi.main.MainViewModeler
import com.pyamsoft.tetherfi.main.SystemBars
import javax.inject.Inject

class ProxyTileActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_TetherFi_Tile)
    super.onCreate(savedInstanceState)

    ObjectGraph.ApplicationScope.retrieve(this).plusTile().create().inject(this)
    stableLayoutHideNavigation()

    val vm = viewModel.requireNotNull()

    setContent {
      val state = vm.state
      val theme by state.theme.collectAsState()

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
    viewModel.requireNotNull().handleSyncDarkTheme(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel = null
  }
}
