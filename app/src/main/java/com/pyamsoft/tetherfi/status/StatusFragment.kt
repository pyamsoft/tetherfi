package com.pyamsoft.tetherfi.status

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.theme.asThemeProvider
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.main.MainComponent
import com.pyamsoft.tetherfi.service.ProxyService
import javax.inject.Inject
import timber.log.Timber

class StatusFragment : Fragment() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null
  @JvmField @Inject internal var theming: Theming? = null

  private var permissionCallback: ActivityResultLauncher<Array<String>>? = null
  private var compose: ViewWindowInsetObserver? = null

  private fun handleToggleProxy() {
    val ctx = requireContext()
    viewModel
        .requireNotNull()
        .handleToggleProxy(
            scope = viewLifecycleOwner.lifecycleScope,
            onStart = { ProxyService.start(ctx) },
            onStop = { ProxyService.stop(ctx) },
        )
  }

  private fun handleSsidChanged(ssid: String) {
    viewModel
        .requireNotNull()
        .handleSsidChanged(
            scope = viewLifecycleOwner.lifecycleScope,
            ssid = ssid.trim(),
        )
  }

  private fun handlePasswordChanged(password: String) {
    viewModel
        .requireNotNull()
        .handlePasswordChanged(
            scope = viewLifecycleOwner.lifecycleScope,
            password = password,
        )
  }

  private fun handlePortChanged(port: String) {
    viewModel
        .requireNotNull()
        .handlePortChanged(
            scope = viewLifecycleOwner.lifecycleScope,
            port = port,
        )
  }

  private fun safeOpenSettingsIntent(action: String) {
    val act = requireActivity()

    // Try specific first, may fail on some devices
    try {
      val intent = Intent(action, "package:${act.packageName}".toUri())
      act.startActivity(intent)
    } catch (e: Throwable) {
      Timber.e(e, "Failed specific intent for $action")
      val intent = Intent(action)
      act.startActivity(intent)
    }
  }

  private fun handleOpenBatterySettings() {
    safeOpenSettingsIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  }

  private fun handleRequestPermissions() {
    viewModel.requireNotNull().also { vm ->
      // Close dialog
      vm.handlePermissionsExplained()

      // Request permissions
      vm.handleRequestPermissions { permissions ->
        Timber.d("Requesting permission for WiDi network: $permissions")
        permissionCallback.requireNotNull().launch(permissions.toTypedArray())
      }
    }
  }

  private fun handleOpenApplicationSettings() {
    // Close dialog
    viewModel.requireNotNull().handlePermissionsExplained()

    // Open settings
    safeOpenSettingsIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()
    Injector.obtainFromActivity<MainComponent>(act).plusStatus().create().inject(this)

    val vm = viewModel.requireNotNull()
    val appName = act.getString(R.string.app_name)

    val themeProvider = act.asThemeProvider(theming.requireNotNull())
    return ComposeView(act).apply {
      id = R.id.screen_status

      val observer = ViewWindowInsetObserver(this)
      val windowInsets = observer.start()
      compose = observer

      setContent {
        vm.Render { state ->
          act.TetherFiTheme(themeProvider) {
            CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
              StatusScreen(
                  modifier = Modifier.fillMaxSize(),
                  state = state,
                  appName = appName,
                  onToggle = { handleToggleProxy() },
                  onSsidChanged = { handleSsidChanged(it) },
                  onPasswordChanged = { handlePasswordChanged(it) },
                  onPortChanged = { handlePortChanged(it) },
                  onOpenBatterySettings = { handleOpenBatterySettings() },
                  onDismissPermissionExplanation = { vm.handlePermissionsExplained() },
                  onRequestPermissions = { handleRequestPermissions() },
                  onOpenPermissionSettings = { handleOpenApplicationSettings() },
              )
            }
          }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.requireNotNull().also { vm ->
      vm.restoreState(savedInstanceState)
      vm.watchStatusUpdates(scope = viewLifecycleOwner.lifecycleScope)
      vm.refreshGroupInfo(scope = viewLifecycleOwner.lifecycleScope)
      vm.loadPreferences(scope = viewLifecycleOwner.lifecycleScope)
    }

    // Register here to watch for permissions
    permissionCallback =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
          var hasPermission = true
          for (entry in results) {
            val permission = entry.key
            val granted = entry.value
            if (!granted) {
              Timber.w("Permission was not granted: $permission")
              hasPermission = false
            }
          }

          if (hasPermission) {
            Timber.d("All permissions are granted, toggle proxy again!")
            handleToggleProxy()
          } else {
            Timber.w("Permissions not granted, cannot toggle proxy")
          }
        }
  }

  override fun onResume() {
    super.onResume()
    viewModel.requireNotNull().refreshSystemInfo(scope = viewLifecycleOwner.lifecycleScope)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewModel?.saveState(outState)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    recompose()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dispose()
    permissionCallback?.unregister()

    compose?.stop()
    compose = null

    theming = null
    viewModel = null
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return StatusFragment().apply { arguments = Bundle().apply {} }
    }
  }
}
