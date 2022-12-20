package com.pyamsoft.tetherfi.tile

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.databinding.ActivityMainBinding
import com.pyamsoft.tetherfi.main.MainViewModeler
import com.pyamsoft.tetherfi.main.SystemBars
import javax.inject.Inject

class ProxyTileActivity : AppCompatActivity() {

  @Inject @JvmField internal var mainViewModel: MainViewModeler? = null
  @Inject @JvmField internal var viewModel: ProxyTileViewModeler? = null

  private var viewBinding: ActivityMainBinding? = null

  private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }

  override fun onCreate(savedInstanceState: Bundle?) {
    // NOTE(Peter):
    // Not full Compose yet
    // Compose has an issue handling Fragments.
    //
    // We need an AndroidView to handle a Fragment, but a Fragment outlives the Activity via the
    // FragmentManager keeping state. The Compose render does not, so when an activity dies from
    // configuration change, the Fragment is headless somewhere in the great beyond. This leads to
    // memory leaks and other issues like Disposable hooks not being called on DisposeEffect blocks.
    // To avoid these growing pains, we use an Activity layout file and then host the ComposeViews
    // from it that are then used to render Activity level views. Fragment transactions happen as
    // normal and then Fragments host ComposeViews too.
    val binding = ActivityMainBinding.inflate(layoutInflater).apply { viewBinding = this }
    setContentView(binding.root)

    setTheme(R.style.Theme_TetherFi_Tile)
    super.onCreate(savedInstanceState)
    ObjectGraph.ApplicationScope.retrieve(this).plusTile().create().inject(this)

    stableLayoutHideNavigation()

    val mainVm = mainViewModel.requireNotNull()
    val vm = viewModel.requireNotNull()

    mainVm.restoreState(savedInstanceState)
    vm.restoreState(savedInstanceState)

    binding.mainTopBar.setContent {
      val mainState = mainVm.state()
      SystemBars()

      TetherFiTheme(mainState.theme) {
        ProxyTileScreen(
            modifier = Modifier.fillMaxSize(),
            state = vm.state(),
            onDismissed = { vm.handleDismissed() },
            onComplete = { finishAndRemoveTask() },
            onStatusUpdated = { ProxyTileService.updateTile(this) },
        )
      }
    }

    mainVm.handleSyncDarkTheme(this)
    vm.bind(scope = lifecycleScope)

    // Wait a little bit before starting the proxy
    handler.postDelayed({ vm.handleToggleProxy() }, 500)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    mainViewModel?.handleSyncDarkTheme(this)
    viewBinding?.apply { mainTopBar.recompose() }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    mainViewModel?.saveState(outState)
    viewModel?.saveState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewBinding?.apply { mainTopBar.dispose() }
    handler.removeCallbacksAndMessages(null)

    viewBinding = null
    viewModel = null
    mainViewModel = null
  }
}
