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
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.foreground.ForegroundWatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var foregroundWatcher: ForegroundWatcher? = null
  @Inject @JvmField internal var launcher: ServiceLauncher? = null

  private val scope by
      lazy(LazyThreadSafetyMode.NONE) {
        CoroutineScope(
            context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
        )
      }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)
    Timber.d("Creating service")

    // Prepare proxy on create
    scope.launch(context = Dispatchers.Default) {
      foregroundWatcher
          .requireNotNull()
          .bind(
              onRefreshNotification = {
                // Do nothing
              },
              onShutdownService = {
                Timber.d("Shutdown event received. Stopping service")
                stopSelf()
              },
          )
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Each time the service starts/restarts we use the fact that it is tied to the Android OS
    // lifecycle to restart the launcher which does all the Proxy lifting.
    launcher.requireNotNull().startForeground()

    // Just start sticky here
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.d("Destroying service")

    scope.cancelChildren()

    foregroundWatcher = null
    launcher = null
  }
}
