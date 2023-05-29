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
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import timber.log.Timber
import javax.inject.Inject

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null
  @Inject @JvmField internal var wiDiReceiverRegister: WiDiReceiverRegister? = null

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
    Timber.d("Starting Proxy!")
    foregroundHandler.requireNotNull().startProxy()
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")

    notificationLauncher?.stop(this)
    foregroundHandler?.destroy()
    wiDiReceiverRegister?.unregister()

    foregroundHandler = null
    notificationLauncher = null
    wiDiReceiverRegister = null
  }
}
