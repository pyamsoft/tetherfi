/*
 * Copyright 2025 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal class ProxyTileInjector : ComposableInjector() {

  @Inject @JvmField internal var viewModel: ProxyTileViewModeler? = null

  override fun onInject(activity: ComponentActivity) {
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
    tileAction: ProxyTileAction,
    onStartProxy: () -> Unit,
    onStopProxy: () -> Unit,
    onToggleProxy: () -> Unit,
) {
  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
  val handleStartProxy by rememberUpdatedState(onStartProxy)
  val handleStopProxy by rememberUpdatedState(onStopProxy)

  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(
      viewModel,
      tileAction,
  ) {
    delay(500)
    when (tileAction) {
      ProxyTileAction.TOGGLE -> handleToggleProxy()
      ProxyTileAction.START -> handleStartProxy()
      ProxyTileAction.STOP -> handleStopProxy()
    }
  }

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }
}

@Composable
fun ProxyTileEntry(
    modifier: Modifier = Modifier,
    appName: String,
    tileAction: ProxyTileAction,
    onComplete: () -> Unit,
    onUpdateTile: (RunningStatus) -> Unit,
) {
  val component = rememberComposableInjector { ProxyTileInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  // If the proxy does not act, we wait a little bit and then close
  val handleComplete: suspend () -> Unit by rememberUpdatedState {
    delay(1.seconds)
    withContext(context = Dispatchers.Main) { onComplete() }
  }

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
      tileAction = tileAction,
      onStartProxy = { viewModel.handleStartProxy { handleComplete() } },
      onStopProxy = { viewModel.handleStopProxy { handleComplete() } },
      onToggleProxy = { viewModel.handleToggleProxy { handleComplete() } },
  )

  ProxyTileScreen(
      modifier = modifier,
      appName = appName,
      state = viewModel,
      onDismissed = { viewModel.handleDismissed() },
      onComplete = onComplete,
      onStatusUpdated = onUpdateTile,
  )
}
