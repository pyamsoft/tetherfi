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

package com.pyamsoft.tetherfi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.cancelChildren
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.foreground.ForegroundLauncher
import com.pyamsoft.tetherfi.service.foreground.ForegroundWatcher
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
    private val foregroundServiceClass: Class<out Service>,
    private val notificationLauncher: NotificationLauncher,
    private val wiDiReceiverRegister: WiDiReceiverRegister,
    private val foregroundWatcher: ForegroundWatcher,
    private val foregroundLauncher: ForegroundLauncher,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, foregroundServiceClass) }

  private val runningState = MutableStateFlow(false)

  private val scope =
      CoroutineScope(
          context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
      )

  private suspend fun stopProxy() {
    notificationLauncher.stop()
    wiDiReceiverRegister.unregister()
  }

  private fun CoroutineScope.startProxy() {
    val scope = this

    // Register for WiDi events
    wiDiReceiverRegister.register()

    // Start notification first for Android O immediately
    scope.launch(context = Dispatchers.Default) { notificationLauncher.start() }

    // Prepare proxy on create
    scope.launch(context = Dispatchers.Default) {
      foregroundWatcher.bind(
          onRefreshNotification = {
            Timber.d("Refresh event received, start notification again")
            notificationLauncher.start()
          },
          onShutdownService = {
            Timber.d("Shutdown event received. Stopping service")
            stopForeground()
          },
      )
    }

    // We leave the launch call in here so that the service lifecycle is 1-1 tied to the hotspot
    // network
    //
    // Since this is not immediate, we check that the service is infact still alive
    scope.launch(context = Dispatchers.Default) {
      Timber.d("Starting Proxy!")
      foregroundLauncher.startProxy()
    }
  }

  @CheckResult
  private suspend fun handleStartService(): Job =
      withContext(context = Dispatchers.Main) {
        launch(context = Dispatchers.Main) {
          startAndroidService()
          startProxy()
        }
      }

  @CheckResult
  private suspend fun handleStopService(): Job =
      withContext(context = Dispatchers.Main + NonCancellable) {
        launch(context = Dispatchers.Main + NonCancellable) {
          stopAndroidService()
          stopProxy()
        }
      }

  private fun startAndroidService() {
    Timber.d("Start Foreground Service!")
    context.startService(foregroundService)
  }

  private fun stopAndroidService() {
    Timber.d("Stop Foreground Service!")
    context.stopService(foregroundService)
  }

  /** Start the service */
  fun startForeground() {
    if (runningState.compareAndSet(expect = false, update = true)) {
      scope.cancelChildren()
      scope.launch { handleStartService() }
    }
  }

  /** Stop the service */
  fun stopForeground() {
    if (runningState.compareAndSet(expect = true, update = false)) {
      scope.cancelChildren()
      scope.launch { handleStopService() }
    }
  }

  /** If the hotspot is in error state, we reset it so that it can start again */
  fun resetError() {
    stopForeground()
  }

  /** If the launcher state is started, we ensure that the service is started too */
  suspend fun ensureAndroidServiceStartedWhenNeeded() =
      withContext(context = Dispatchers.Main) {
        if (runningState.value) {
          Timber.d("ServiceLauncher reports Active. Ensure Android service!")
          startAndroidService()
        }
      }
}
