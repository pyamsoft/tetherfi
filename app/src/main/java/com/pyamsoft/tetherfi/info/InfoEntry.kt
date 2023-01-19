package com.pyamsoft.tetherfi.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
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
private fun MountHooks(viewModel: InfoViewModeler) {
  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }

  LifecycleEffect {
    object : DefaultLifecycleObserver {

      override fun onResume(owner: LifecycleOwner) {
        viewModel.refreshConnectionInfo(scope = owner.lifecycleScope)
      }
    }
  }
}

@Composable
fun InfoEntry(
    modifier: Modifier = Modifier,
    appName: String,
) {
  val component = rememberComposableInjector { InfoInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  MountHooks(
      viewModel = viewModel,
  )

  InfoScreen(
      modifier = modifier,
      state = viewModel.state,
      appName = appName,
  )
}
