package com.pyamsoft.tetherfi.info

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
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.theme.asThemeProvider
import com.pyamsoft.pydroid.ui.util.dispose
import com.pyamsoft.pydroid.ui.util.recompose
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiTheme
import com.pyamsoft.tetherfi.main.MainView
import javax.inject.Inject

class InfoFragment : Fragment(), FragmentNavigator.Screen<MainView> {

  @JvmField @Inject internal var theming: Theming? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val act = requireActivity()
    ObjectGraph.ActivityScope.retrieve(act).plusInfo().create().inject(this)

    val appName = act.getString(R.string.app_name)

    val themeProvider = act.asThemeProvider(theming.requireNotNull())
    return ComposeView(act).apply {
      id = R.id.screen_status

      setContent {
        act.TetherFiTheme(themeProvider) {
          InfoEntry(
              modifier = Modifier.fillMaxSize(),
              appName = appName,
          )
        }
      }
    }
  }

//  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//    viewModel.requireNotNull().also { vm ->
//      vm.restoreState(savedInstanceState)
//    }
//  }

  override fun onResume() {
    super.onResume()
    requireActivity().reportFullyDrawn()
  }

//  override fun onSaveInstanceState(outState: Bundle) {
//    super.onSaveInstanceState(outState)
//    viewModel?.saveState(outState)
//  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    recompose()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    dispose()

    theming = null
  }

  override fun getScreenId(): MainView {
    return MainView.Info
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return InfoFragment().apply { arguments = Bundle().apply {} }
    }
  }
}
