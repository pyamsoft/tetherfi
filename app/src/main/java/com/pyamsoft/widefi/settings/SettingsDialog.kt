package com.pyamsoft.widefi.settings

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.app.makeFullscreen
import com.pyamsoft.pydroid.ui.navigator.Navigator
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.widefi.WidefiTheme
import com.pyamsoft.widefi.databinding.DialogSettingsBinding
import com.pyamsoft.widefi.main.MainComponent
import javax.inject.Inject

class SettingsDialog : AppCompatDialogFragment() {

  @Inject @JvmField internal var navigator: Navigator<SettingsPage>? = null
  @Inject @JvmField internal var theming: Theming? = null
  @Inject @JvmField internal var viewModel: SettingsViewModeler? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()
    val binding = DialogSettingsBinding.inflate(inflater, container, false)

    Injector.obtainFromActivity<MainComponent>(act)
        .plusSettings()
        .create(this, binding.dialogSettings.id)
        .inject(this)

    val vm = viewModel.requireNotNull()
    val themeProvider = ThemeProvider { theming.requireNotNull().isDarkTheme(act) }

    binding.dialogComposeTop.setContent {
      act.WidefiTheme(themeProvider) {
        SettingsToolbar(
            modifier = Modifier.fillMaxWidth().onSizeChanged { vm.handleTopBarHeight(it.height) },
            onClose = { dismiss() },
        )
      }
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    makeFullscreen()

    viewModel.requireNotNull().restoreState(savedInstanceState)

    navigator.requireNotNull().also { n ->
      n.restoreState(savedInstanceState)
      n.loadIfEmpty { SettingsPage.Settings.asScreen() }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    makeFullscreen()
    recompose()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    viewModel?.saveState(outState)
    navigator?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dispose()

    navigator = null
    theming = null
    viewModel = null
  }

  companion object {

    private const val TAG = "SettingsDialog"

    @JvmStatic
    @CheckResult
    private fun newInstance(): DialogFragment {
      return SettingsDialog().apply { arguments = Bundle.EMPTY }
    }

    @JvmStatic
    fun show(activity: FragmentActivity) {
      return newInstance().show(activity, TAG)
    }
  }
}
