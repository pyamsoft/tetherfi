package com.pyamsoft.tetherfi.error

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.WidefiTheme
import com.pyamsoft.tetherfi.main.MainComponent
import javax.inject.Inject

class ErrorFragment : Fragment() {

  @JvmField @Inject internal var viewModel: ErrorViewModeler? = null
  @JvmField @Inject internal var theming: Theming? = null

  private var windowInsetObserver: ViewWindowInsetObserver? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()
    Injector.obtainFromActivity<MainComponent>(act).plusError().create().inject(this)

    val vm = viewModel.requireNotNull()

    val themeProvider = ThemeProvider { theming.requireNotNull().isDarkTheme(act) }
    return ComposeView(act).apply {
      id = R.id.screen_activity

      val observer = ViewWindowInsetObserver(this)
      val windowInsets = observer.start()
      windowInsetObserver = observer

      setContent {
        vm.Render { state ->
          act.WidefiTheme(themeProvider) {
            CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
              ErrorScreen(
                  modifier = Modifier.fillMaxSize(),
                  state = state,
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
      vm.watchNetworkActivity(scope = viewLifecycleOwner.lifecycleScope)
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

    windowInsetObserver?.stop()
    windowInsetObserver = null

    theming = null
    viewModel = null
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return ErrorFragment().apply { arguments = Bundle().apply {} }
    }
  }
}
