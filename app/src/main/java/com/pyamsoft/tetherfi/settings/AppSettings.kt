package com.pyamsoft.tetherfi.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.fragment.app.Fragment
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.ZeroSize
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import com.pyamsoft.pydroid.ui.preference.Preferences
import com.pyamsoft.pydroid.ui.settings.SettingsFragment
import com.pyamsoft.tetherfi.ObjectGraph
import javax.inject.Inject

internal class AppSettings : SettingsFragment(), FragmentNavigator.Screen<SettingsPage> {

  override val hideClearAll: Boolean = false

  override val hideUpgradeInformation: Boolean = false

  @Inject @JvmField internal var viewModel: SettingsViewModeler? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ObjectGraph.ActivityScope.retrieve(requireActivity()).plusAppSettings().create().inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.requireNotNull().restoreState(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewModel?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    viewModel = null
  }

  @Composable
  override fun customElevation(): Dp {
    return DialogDefaults.Elevation
  }

  @Composable
  override fun customBottomItemMargin(): Dp {
    return ZeroSize
  }

  @Composable
  override fun customPostPreferences(): List<Preferences> {
    return emptyList()
  }

  @Composable
  override fun customPrePreferences(): List<Preferences> {
    return emptyList()
  }

  @Composable
  override fun customTopItemMargin(): Dp {
    val state = viewModel.requireNotNull().state()

    val density = LocalDensity.current
    val height = state.topBarOffset
    return remember(density, height) { density.run { height.toDp() } }
  }

  override fun getScreenId(): SettingsPage {
    return SettingsPage.Settings
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return AppSettings().apply { arguments = Bundle.EMPTY }
    }
  }
}
