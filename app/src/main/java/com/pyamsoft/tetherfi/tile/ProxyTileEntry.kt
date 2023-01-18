package com.pyamsoft.tetherfi.tile

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberActivity
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import javax.inject.Inject

internal class ProxyTileInjector : ComposableInjector() {

  @Inject @JvmField internal var viewModel: ProxyTileViewModeler? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ApplicationScope.retrieve(activity).plusTile().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    component: ProxyTileInjector,
    onToggleProxy: () -> Unit,
) {
  val viewModel = rememberNotNull(component.viewModel)

  DisposableEffect(
      viewModel,
  ) {
    val handler = Handler(Looper.getMainLooper())

    // Wait a little bit before starting the proxy
    handler.postDelayed(onToggleProxy, 500)

    onDispose { handler.removeCallbacksAndMessages(null) }
  }

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }
}

@Composable
fun ProxyTileEntry(
    modifier: Modifier = Modifier,
) {
  val component = rememberComposableInjector { ProxyTileInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  // Hooks that run on mount
  MountHooks(
      component = component,
      onToggleProxy = { viewModel.handleToggleProxy() },
  )

  val activity = rememberActivity()

  ProxyTileScreen(
      modifier = modifier,
      state = viewModel.state(),
      onDismissed = { viewModel.handleDismissed() },
      onComplete = { activity.finishAndRemoveTask() },
      onStatusUpdated = { ProxyTileService.updateTile(activity) },
  )
}
