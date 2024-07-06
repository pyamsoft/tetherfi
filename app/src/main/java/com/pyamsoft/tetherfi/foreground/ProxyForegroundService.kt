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
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.service.ServiceRunner
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notification: NotificationLauncher? = null

  @Inject @JvmField internal var runner: ServiceRunner? = null

  private var scope: CoroutineScope? = null

  @CheckResult
  private fun makeScope(): CoroutineScope {
    return CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
    )
  }

  @CheckResult
  private fun ensureScope(): CoroutineScope {
    return (scope ?: makeScope()).also { scope = it }
  }

  private fun startRunner() {
    runner
        .requireNotNull()
        .start(
            scope = ensureScope(),
        )
  }

  private fun startNotification() {
    notification
        .requireNotNull()
        .startForeground(
            scope = ensureScope(),
            service = this,
        )
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)

    Timber.d { "Creating service" }

    // Ensure the notification is started
    startNotification()
  }

  /**
   * If the app is in the background, this will not run unless the app sets Battery Optimization off
   */
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Ensure the notification is started
    startNotification()

    // Each time the service starts/restarts we use the fact that it is tied to the Android OS
    // lifecycle to restart the launcher which does all the Proxy lifting.
    startRunner()

    // Just start sticky here
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.d { "Destroying service" }

    scope?.cancel()

    scope = null
    runner = null
  }
}
