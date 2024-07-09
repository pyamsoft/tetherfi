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

package com.pyamsoft.tetherfi.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.service.ServiceScope
import javax.inject.Inject

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var serviceScope: ServiceScope? = null

  private fun start() {
    serviceScope.requireNotNull().start(service = this)
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)

    Timber.d { "Creating service" }

    start()
  }

  /**
   * If the app is in the background, this will not run unless the app sets Battery Optimization off
   */
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d { "Start command received" }
    start()

    // Just start sticky here
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.d { "Destroying service" }

    serviceScope?.cancel()
    serviceScope = null
  }
}
