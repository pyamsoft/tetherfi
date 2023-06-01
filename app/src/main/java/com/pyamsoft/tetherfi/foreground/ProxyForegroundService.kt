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
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.cancelChildren
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null
  @Inject @JvmField internal var wiDiReceiverRegister: WiDiReceiverRegister? = null

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
    )
  }

  private var isServiceAlive = false

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)

    Timber.d("Creating service")
    isServiceAlive = true

    // Start notification first for Android O immediately
    notificationLauncher.requireNotNull().start(this)

    // Register for WiDi events
    wiDiReceiverRegister.requireNotNull().register()

    // Prepare proxy on create
    foregroundHandler
        .requireNotNull()
        .bind(
            scope = scope,
            onRefreshNotification = {
              if (!isServiceAlive) {
                Timber.w("Refresh notification received but service was dead")
                return@bind
              }

              Timber.d("Refresh event received, start notification again")
              notificationLauncher.requireNotNull().start(this)
            },
            onShutdownService = {
              Timber.d("Shutdown event received. Stopping service")
              stopSelf()
            },
        )

    // We leave the launch call in here so that the service lifecycle is 1-1 tied to the hotspot
    // network
    //
    // Since this is not immediate, we check that the service is infact still alive
    scope.launch {
      if (!isServiceAlive) {
        Timber.w("Proxy start() called but service is not alive!")
        return@launch
      }

      Timber.d("Starting Proxy!")
      foregroundHandler.requireNotNull().startProxy()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Just start sticky here
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")
    isServiceAlive = false

    notificationLauncher?.stop(this)
    wiDiReceiverRegister?.unregister()

    // Cancel all children but not this scope
    scope.cancelChildren()

    foregroundHandler = null
    notificationLauncher = null
    wiDiReceiverRegister = null
  }
}
