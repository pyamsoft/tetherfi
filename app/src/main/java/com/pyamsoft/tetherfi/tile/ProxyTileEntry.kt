/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.tile

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
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
    viewModel: ProxyTileViewModeler,
    onToggleProxy: () -> Unit,
) {
  val handleToggleProxy by rememberUpdatedState(onToggleProxy)

  SaveStateDisposableEffect(viewModel)

  DisposableEffect(
      viewModel,
  ) {
    val handler = Handler(Looper.getMainLooper())

    // Wait a little bit before starting the proxy
    handler.postDelayed({ handleToggleProxy() }, 500)

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
      viewModel = viewModel,
      onToggleProxy = { viewModel.handleToggleProxy() },
  )

  val activity = rememberActivity()

  ProxyTileScreen(
      modifier = modifier,
      state = viewModel.state,
      onDismissed = { viewModel.handleDismissed() },
      onComplete = { activity.finishAndRemoveTask() },
      onStatusUpdated = { ProxyTileService.updateTile(activity) },
  )
}
