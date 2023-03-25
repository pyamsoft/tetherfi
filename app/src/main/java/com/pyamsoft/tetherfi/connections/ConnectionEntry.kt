package com.pyamsoft.tetherfi.connections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject

internal class ConnectionInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: ConnectionViewModel? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusConnection().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    component: ConnectionInjector,
) {
  val viewModel = rememberNotNull(component.viewModel)

  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }
}

@Composable
fun ConnectionEntry(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,
) {
  val component = rememberComposableInjector { ConnectionInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  // Hooks that run on mount
  MountHooks(
      component = component,
  )

  ConnectionScreen(
      modifier = modifier,
      state = viewModel.state,
      serverViewState = serverViewState,
      onToggleBlock = { viewModel.handleToggleBlock(it) },
  )
}
