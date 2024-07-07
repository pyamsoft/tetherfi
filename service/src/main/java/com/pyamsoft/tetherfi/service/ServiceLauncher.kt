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

package com.pyamsoft.tetherfi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerStopBroadcaster
import com.pyamsoft.tetherfi.server.broadcast.BroadcastStatus
import com.pyamsoft.tetherfi.server.proxy.ProxyStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
    private val foregroundServiceClass: Class<out Service>,
    private val wiDiStatus: BroadcastStatus,
    private val proxyStatus: ProxyStatus,
    private val stopper: ServerStopBroadcaster,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, foregroundServiceClass) }

  /** Start the service */
  fun startForeground() {
    Timber.d { "Start Foreground Service!" }
    ContextCompat.startForegroundService(context, foregroundService)
  }

  /** Stop the service */
  suspend fun stopForeground() =
      withContext(context = Dispatchers.Default) {
        // When we have a lot of sockets open, cancelling the coroutine scope can take a very long
        // time. When this happens, the status does not correctly update, since the coroutine cancel
        // happens in a structured but non defined and non controllable order.
        //
        // Avoid this UI hang appearance by broadcasting a stopping command first and then tearing
        // down the scope to run the actual stop code
        Timber.d { "Prepare service stop" }
        stopper.stop()

        withContext(context = Dispatchers.Main) {
          Timber.d { "Stop Foreground Service!" }
          context.stopService(foregroundService)
        }
      }

  /** If the hotspot is in error state, we reset it so that it can start again */
  suspend fun resetError() =
      withContext(context = Dispatchers.Default) {
        stopForeground()

        // Reset status after shutdown
        Timber.d { "Resetting network Status" }
        wiDiStatus.set(RunningStatus.NotRunning, clearError = true)
        proxyStatus.set(RunningStatus.NotRunning, clearError = true)
      }
}
