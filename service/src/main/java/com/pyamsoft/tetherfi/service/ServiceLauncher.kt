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
import android.os.Build
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import javax.inject.Inject
import timber.log.Timber

class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
    private val foregroundServiceClass: Class<out Service>,
    private val foregroundHandler: ForegroundHandler,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, foregroundServiceClass) }

  fun startForeground() {
    Timber.d("Start Foreground Service!")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(foregroundService)
    } else {
      context.startService(foregroundService)
    }
  }

  fun stopForeground() {
    // Instead of stopping the service, stop the widi network
    // as this will eventually fire a shutdown() event which will stop the service
    // if it is still running
    Timber.d("Stop Foreground Service!")
    context.stopService(foregroundService)

    // Also directly stop the network
    //
    // If you get into a network error state where you never initially launched the service to begin
    // with, stuff will be perma locked.
    Timber.d("Directly calling stop on the network to avoid an Error-Lock state")
    foregroundHandler.stopProxy()
  }
}
