/*
 * Copyright 2024 pyamsoft
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

package com.pyamsoft.tetherfi.connections

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject

internal class ConnectionInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: ConnectionViewModel? = null

  override fun onInject(activity: ComponentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusConnection().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    viewModel: ConnectionViewModel,
) {
  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(viewModel) { viewModel.bind(scope = this) }
}

@Composable
fun ConnectionEntry(
    modifier: Modifier = Modifier,
    appName: String,
    serverViewState: ServerViewState,
) {
  val component = rememberComposableInjector { ConnectionInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  // Use the LifecycleOwner.CoroutineScope (Activity usually)
  // so that the scope does not die because of navigation events
  val owner = LocalLifecycleOwner.current
  val lifecycleScope = owner.lifecycleScope

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
  )

  ConnectionScreen(
      modifier = modifier,
      appName = appName,
      state = viewModel,
      serverViewState = serverViewState,
      onToggleBlock = { viewModel.handleToggleBlock(it) },
      onOpenManageNickName = { viewModel.handleOpenManageNickName(it) },
      onCloseManageNickName = { viewModel.handleCloseManageNickName() },
      onOpenManageTransferLimit = { viewModel.handleOpenManageTransferLimit(it) },
      onCloseManageTransferLimit = { viewModel.handleCloseManageTransferLimit() },
      onUpdateNickName = {
        viewModel.handleUpdateNickName(
            scope = lifecycleScope,
            nickName = it,
        )
      },
      onUpdateTransferLimit = {
        viewModel.handleUpdateTransferLimit(
            scope = lifecycleScope,
            limit = it,
        )
      },
  )
}
