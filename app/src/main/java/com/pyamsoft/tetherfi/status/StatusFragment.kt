package com.pyamsoft.tetherfi.status

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.theme.asThemeProvider
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.main.MainView
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class StatusFragment : Fragment(), FragmentNavigator.Screen<MainView> {

  @JvmField @Inject internal var theming: Theming? = null
  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null

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

  private fun registerPermissionRequests() {
    serverRequester?.unregister()
    notificationRequester?.unregister()

    serverRequester =
        serverPermissionRequester.requireNotNull().registerRequester(this) { granted ->
          if (granted) {
            Timber.d("Network permission granted, toggle proxy")

            // Broadcast in the background
            viewLifecycleOwner.lifecycleScope.launch(context = Dispatchers.IO) {
              permissionRequestBus.requireNotNull().send(PermissionRequests.ToggleProxy)
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
            viewLifecycleOwner.lifecycleScope.launch(context = Dispatchers.IO) {
              permissionRequestBus
                  .requireNotNull()
                  .send(PermissionRequests.RefreshNotificationPermission)
            }
          } else {
            Timber.w("Notification permission not granted")
          }
        }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()

    ObjectGraph.ActivityScope.retrieve(act).plusStatus().create().inject(this)

    val appName = act.getString(R.string.app_name)

    // As early as possible because of Lifecycle quirks
    registerPermissionRequests()

    val themeProvider = act.asThemeProvider(theming.requireNotNull())
    return ComposeView(act).apply {
      id = R.id.screen_status

      setContent {
        act.TetherFiTheme(themeProvider) {
          StatusEntry(
              modifier = Modifier.fillMaxSize(),
              appName = appName,
              serverRequester = rememberNotNull(serverRequester),
              notificationRequester = rememberNotNull(notificationRequester),
          )
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    recompose()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dispose()

    notificationRequester?.unregister()
    serverRequester?.unregister()

    serverPermissionRequester = null
    notificationPermissionRequester = null
    theming = null
    permissionRequestBus = null
    serverRequester = null
    notificationRequester = null
  }

  override fun getScreenId(): MainView {
    return MainView.Status
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return StatusFragment().apply { arguments = Bundle().apply {} }
    }
  }
}
