package com.pyamsoft.widefi.status

import android.content.res.Configuration
import android.os.Bundle
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
import com.pyamsoft.widefi.R
import com.pyamsoft.widefi.WidefiTheme
import com.pyamsoft.widefi.main.MainComponent
import com.pyamsoft.widefi.service.ProxyService
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
            onRequestPermissions = { permissions ->
              Timber.d("Requesting permission for WiDi network")
              permissionCallback.requireNotNull().launch(permissions.toTypedArray())
            },
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

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()
    Injector.obtainFromActivity<MainComponent>(act).plusStatus().create().inject(this)

    val vm = viewModel.requireNotNull()

    val themeProvider = act.asThemeProvider(theming.requireNotNull())
    return ComposeView(act).apply {
      id = R.id.screen_status

      val observer = ViewWindowInsetObserver(this)
      val windowInsets = observer.start()
      compose = observer

      setContent {
        vm.Render { state ->
          act.WidefiTheme(themeProvider) {
            CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
              StatusScreen(
                  modifier = Modifier.fillMaxSize(),
                  state = state,
                  onToggle = { handleToggleProxy() },
                  onSsidChanged = { handleSsidChanged(it) },
                  onPasswordChanged = { handlePasswordChanged(it) },
                  onPortChanged = { handlePortChanged(it) },
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
          }
        }
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
