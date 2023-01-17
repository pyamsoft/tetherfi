package com.pyamsoft.tetherfi.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.installPYDroid
import com.pyamsoft.pydroid.ui.changelog.ChangeLogProvider
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.pydroid.util.doOnCreate
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import com.pyamsoft.tetherfi.ui.InstallPYDroidExtras
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

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

    doOnCreate { registerToSendPermissionResults() }

    doOnCreate { registerToRespondToPermissionRequests() }
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
    super.onCreate(savedInstanceState)
    stableLayoutHideNavigation()

    val component = ObjectGraph.ApplicationScope.retrieve(this).plusMain().create()
    component.inject(this)
    ObjectGraph.ActivityScope.install(this, component)

    val vm = viewModel.requireNotNull()

    val appName = getString(R.string.app_name)
    setContent {
      val state = vm.state()

      TetherFiTheme(
          theme = state.theme,
      ) {
        SystemBars()
        InstallPYDroidExtras()
        MainEntry(
            modifier = Modifier.fillMaxSize(),
            appName = appName,
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

    val existingComposeView =
        window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as? ComposeView
    existingComposeView?.recompose()
  }

  override fun onDestroy() {
    super.onDestroy()
    val existingComposeView =
        window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as? ComposeView
    existingComposeView?.dispose()

    notificationRequester?.unregister()
    serverRequester?.unregister()

    serverPermissionRequester = null
    notificationPermissionRequester = null
    permissionRequestBus = null
    serverRequester = null
    notificationRequester = null
    viewModel = null
  }
}
