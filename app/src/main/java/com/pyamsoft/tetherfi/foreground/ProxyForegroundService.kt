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

package com.pyamsoft.tetherfi.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.dispatcher.ProxyDispatcher
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null
  @Inject @JvmField internal var wiDiReceiverRegister: WiDiReceiverRegister? = null
  @Inject @JvmField internal var proxyDispatcher: ProxyDispatcher? = null

  private var scope: CoroutineScope? = null

  @CheckResult
  private fun ensureScope(): CoroutineScope {
    scope =
        scope.let { s ->
          if (s == null) {
            return@let CoroutineScope(
                context =
                    SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name))
          } else {
            return@let s
          }
        }
    return scope.requireNotNull()
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)

    Timber.d("Creating service")

    // Start notification first for Android O immediately
    notificationLauncher.requireNotNull().start(this)

    // Register for WiDi events
    wiDiReceiverRegister.requireNotNull().register()

    // Prepare proxy on create
    foregroundHandler
        .requireNotNull()
        .bind(
            scope = ensureScope(),
            onRefreshNotification = {
              Timber.d("Refresh event received, start notification again")
              notificationLauncher.requireNotNull().start(this)
            },
            onShutdownService = {
              Timber.d("Shutdown event received. Stopping service")
              stopSelf()
            },
        )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Constantly attempt to start proxy here instead of in onCreate
    //
    // If we spam ON/OFF, the service is created but the proxy is only started again within this
    // block.
    ensureScope().launch {
      Timber.d("Starting Proxy!")
      foregroundHandler.requireNotNull().startProxy()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")

    notificationLauncher?.stop(this)
    wiDiReceiverRegister?.unregister()

    ensureScope()
        .launch { foregroundHandler?.destroy() }
        .invokeOnCompletion {
          Timber.d("Proxy shutdown complete, killing Service")
          scope?.cancel()
          proxyDispatcher?.shutdown()

          scope = null
          foregroundHandler = null
          notificationLauncher = null
          wiDiReceiverRegister = null
          proxyDispatcher = null
        }
  }
}
