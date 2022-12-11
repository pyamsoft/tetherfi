package com.pyamsoft.tetherfi.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.installPYDroid
import com.pyamsoft.pydroid.ui.changelog.ChangeLogProvider
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.navigator.Navigator
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.util.doOnCreate
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.databinding.ActivityMainBinding
import com.pyamsoft.tetherfi.settings.SettingsDialog
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  @Inject @JvmField internal var navigator: Navigator<MainView>? = null

  private var viewBinding: ActivityMainBinding? = null

  init {
    doOnCreate {
      installPYDroid(
          provider =
              object : ChangeLogProvider {

                override val applicationIcon = R.mipmap.ic_launcher

                override val changelog = buildChangeLog {
                  change("[BETA] Rework Tile to be more reliable. UI still in progress.")
                  bugfix("Allow the setup to restart if the Hotspot enters an error state")
                  bugfix("Remove dead code for size improvement")
                }
              },
      )
    }
  }

  private fun handleOpenApplicationSettings() {
    SettingsDialog.show(this)
  }

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

    val component =
        ObjectGraph.ApplicationScope.retrieve(this)
            .plusMain()
            .create(
                activity = this,
                fragmentContainerId = binding.mainContents.id,
            )
    component.inject(this)
    ObjectGraph.ActivityScope.install(this, component)

    super.onCreate(savedInstanceState)
    stableLayoutHideNavigation()

    val vm = viewModel.requireNotNull()
    val navi = navigator.requireNotNull()
    vm.restoreState(savedInstanceState)

    binding.mainTopBar.setContent {
      val screen by navi.currentScreenState()

      val state = vm.state()

      SystemBars()
      if (screen != null) {
        TetherFiTheme(state.theme) {
          MainTopBar(
              onSettingsOpen = { handleOpenApplicationSettings() },
          )
        }
      }
    }

    vm.handleSyncDarkTheme(this)
    navi.restoreState(savedInstanceState)
    navi.loadIfEmpty { MainView.Status }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    viewModel?.handleSyncDarkTheme(this)
    viewBinding?.apply { mainTopBar.recompose() }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewModel?.saveState(outState)
    navigator?.saveState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewBinding?.apply { mainTopBar.dispose() }

    viewBinding = null
    viewModel = null
    navigator = null
  }
}
