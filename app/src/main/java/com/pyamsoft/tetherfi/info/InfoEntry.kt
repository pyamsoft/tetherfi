package com.pyamsoft.tetherfi.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject

internal class InfoInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: InfoViewModeler? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusInfo().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

@Composable
fun InfoEntry(
    modifier: Modifier = Modifier,
    appName: String,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
) {
  val component = rememberComposableInjector { InfoInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  InfoScreen(
      modifier = modifier,
      state = viewModel.state,
      appName = appName,
      serverViewState = serverViewState,
      onShowQRCode = onShowQRCode,
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
  )
}
