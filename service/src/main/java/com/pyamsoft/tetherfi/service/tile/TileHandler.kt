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

package com.pyamsoft.tetherfi.service.tile

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import javax.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class TileHandler
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
    private val network: WiDiNetworkStatus,
    private val permissionGuard: PermissionGuard,
) {

  private var scope: CoroutineScope? = null

  private fun withScope(block: suspend CoroutineScope.() -> Unit) {
    scope =
        scope.let { s ->
          if (s == null) {
            return@let CoroutineScope(
                context =
                    SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
            )
          } else {
            return@let s
          }
        }

    scope
        .requireNotNull()
        .launch(
            context = Dispatchers.Default,
            block = block,
        )
  }

  private fun CoroutineScope.watchStatusUpdates(
      onNetworkError: (RunningStatus.Error) -> Unit,
      onNetworkNotRunning: () -> Unit,
      onNetworkStarting: () -> Unit,
      onNetworkRunning: () -> Unit,
      onNetworkStopping: () -> Unit,
  ) {
    launch {
      enforcer.assertOffMainThread()

      network.onProxyStatusChanged().also { f ->
        launch {
          enforcer.assertOffMainThread()

          f.collect { status ->
            when (status) {
              is RunningStatus.Error -> {
                Timber.w("Error running Proxy: ${status.message}")
                onNetworkError(status)
              }
              else -> Timber.d("Unhandled Proxy status event $status")
            }
          }
        }
      }

      network.onStatusChanged().also { f ->
        launch(context = Dispatchers.Default) {
          enforcer.assertOffMainThread()

          f.collect { status ->
            when (status) {
              is RunningStatus.Error -> {
                Timber.w("Error running WiDi network: ${status.message}")
                onNetworkError(status)
              }
              is RunningStatus.NotRunning -> onNetworkNotRunning()
              is RunningStatus.Running -> onNetworkRunning()
              is RunningStatus.Starting -> onNetworkStarting()
              is RunningStatus.Stopping -> onNetworkStopping()
            }
          }
        }
      }
    }
  }

  fun destroy() {
    // Kill scope
    scope?.cancel()
    scope = null
  }

  @CheckResult
  fun getNetworkStatus(): RunningStatus {
    if (!permissionGuard.canCreateWiDiNetwork()) {
      return STATUS_MISSING_PERMISSION
    }

    return network.getCurrentStatus()
  }

  fun bind(
      onNetworkError: (RunningStatus.Error) -> Unit,
      onNetworkNotRunning: () -> Unit,
      onNetworkStarting: () -> Unit,
      onNetworkRunning: () -> Unit,
      onNetworkStopping: () -> Unit,
  ) = withScope {
    watchStatusUpdates(
        onNetworkError = onNetworkError,
        onNetworkNotRunning = onNetworkNotRunning,
        onNetworkStarting = onNetworkStarting,
        onNetworkRunning = onNetworkRunning,
        onNetworkStopping = onNetworkStopping,
    )
  }

  companion object {

    private val STATUS_MISSING_PERMISSION = RunningStatus.Error("Missing required permissions.")
  }
}
