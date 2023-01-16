package com.pyamsoft.tetherfi.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.installPYDroid
import com.pyamsoft.pydroid.ui.changelog.ChangeLogProvider
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.navigator.Navigator
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.pydroid.util.doOnCreate
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.databinding.ActivityMainBinding
import com.pyamsoft.tetherfi.settings.SettingsDialog
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  @Inject @JvmField internal var navigator: Navigator<MainView>? = null

  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null
  @JvmField @Inject internal var permissionResponseBus: EventBus<PermissionResponse>? = null

  @JvmField
  @Inject
  @Named("server")
  internal var serverPermissionRequester: PermissionRequester? = null

  @JvmField
  @Inject
  @Named("notification")
  internal var notificationPermissionRequester: PermissionRequester? = null

  private var serverRequester: PermissionRequester.Requester? = null
  private var notificationRequester: PermissionRequester.Requester? = null

  private var viewBinding: ActivityMainBinding? = null

  init {
    doOnCreate {
      installPYDroid(
          provider =
              object : ChangeLogProvider {

                override val applicationIcon = R.mipmap.ic_launcher

                override val changelog = buildChangeLog {
                  change("A more reliable Quick Tile Toggle with new UI")
                  change("Remove broken UDP traffic support")
                  change("More information displayed on status screen")
                  change("Clarify setup instructions")
                  bugfix("Faster TCP performance using native operations")
                  bugfix("Less memory used for hotspot creation")
                }
              },
      )
    }
  }

  private fun registerToSendPermissionResults() {
    serverRequester?.unregister()
    notificationRequester?.unregister()

    serverRequester =
        serverPermissionRequester.requireNotNull().registerRequester(this) { granted ->
          if (granted) {
            Timber.d("Network permission granted, toggle proxy")

            // Broadcast in the background
            lifecycleScope.launch(context = Dispatchers.IO) {
              permissionResponseBus.requireNotNull().send(PermissionResponse.ToggleProxy)
            }
          } else {
            Timber.w("Network permission not granted")
          }
        }

    notificationRequester =
        notificationPermissionRequester.requireNotNull().registerRequester(this) { granted ->
          if (granted) {
            Timber.d("Notification permission granted")

            // Broadcast in the background
            lifecycleScope.launch(context = Dispatchers.IO) {
              permissionResponseBus.requireNotNull().send(PermissionResponse.RefreshNotification)
            }
          } else {
            Timber.w("Notification permission not granted")
          }
        }
  }

  private fun registerToRespondToPermissionRequests() {
    lifecycleScope.launch(context = Dispatchers.IO) {
      permissionRequestBus.requireNotNull().onEvent {
        when (it) {
          is PermissionRequests.Notification -> {
            notificationRequester.requireNotNull().requestPermissions()
          }
          is PermissionRequests.Server -> {
            serverRequester.requireNotNull().requestPermissions()
          }
        }
      }
    }
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

    // Must happen during onCreate or app crashes
    registerToRespondToPermissionRequests()
    registerToSendPermissionResults()

    val vm = viewModel.requireNotNull()
    val navi = navigator.requireNotNull()
    val appName = getString(R.string.app_name)
    vm.restoreState(savedInstanceState)

    binding.mainTopBar.setContent {
      val screen by navi.currentScreenState()

      val state = vm.state()

      SystemBars()
      screen?.also { selected ->
        TetherFiTheme(state.theme) {
          MainTopBar(
              appName = appName,
              selected = selected,
              onSettingsOpen = { SettingsDialog.show(this) },
              onTabSelected = { navi.navigateTo(it) },
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

    notificationRequester?.unregister()
    serverRequester?.unregister()

    serverPermissionRequester = null
    notificationPermissionRequester = null
    permissionRequestBus = null
    serverRequester = null
    notificationRequester = null
    viewBinding = null
    viewModel = null
    navigator = null
  }
}
