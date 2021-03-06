package com.pyamsoft.tetherfi.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.google.accompanist.insets.ProvideWindowInsets
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.app.PYDroidActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.navigator.Navigator
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiComponent
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.databinding.ActivityMainBinding
import com.pyamsoft.tetherfi.settings.SettingsDialog
import javax.inject.Inject

class MainActivity : PYDroidActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  @Inject @JvmField internal var navigator: Navigator<MainView>? = null

  private var viewBinding: ActivityMainBinding? = null
  private var injector: MainComponent? = null

  override val applicationIcon = R.mipmap.ic_launcher

  override val changelog = buildChangeLog {
    feature("Allow changing between Wi-Fi frequency bands")
    feature("Optionally improve performance by keeping a partial CPU wakelock")
    feature("Initial support for UDP traffic in proxy")
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

    injector =
        Injector.obtainFromApplication<TetherFiComponent>(this)
            .plusMain()
            .create(
                activity = this,
                fragmentContainerId = binding.mainContents.id,
            )
            .also { c -> c.inject(this) }

    super.onCreate(savedInstanceState)
    stableLayoutHideNavigation()

    val vm = viewModel.requireNotNull()
    val navi = navigator.requireNotNull()
    vm.restoreState(savedInstanceState)

    binding.mainTopBar.setContent {
      val screen by navi.currentScreenState()

      vm.Render { state ->
        val theme = state.theme

        SystemBars()
        TetherFiTheme(theme) {
          ProvideWindowInsets {
            Box(
                contentAlignment = Alignment.TopCenter,
            ) {
              screen?.let { s ->
                MainTopBar(
                    currentTab = s,
                    onLoadStatus = { navi.navigateTo(MainView.Status) },
                    onLoadActivity = { navi.navigateTo(MainView.Activity) },
                    onLoadError = { navi.navigateTo(MainView.Errors) },
                    onSettingsOpen = { handleOpenApplicationSettings() },
                )
              }
            }
          }
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

  override fun getSystemService(name: String): Any? {
    return if (name == MainComponent::class.java.name) injector else super.getSystemService(name)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewBinding?.apply { mainTopBar.dispose() }
    viewBinding = null

    injector = null
    viewModel = null
    navigator = null
  }
}
