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

package com.pyamsoft.tetherfi.tile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.pyamsoft.tetherfi.core.Timber
import javax.inject.Inject
import javax.inject.Named

internal class DefaultProxyTileActivityLauncher
@Inject
internal constructor(
    @Named("tile_activity") private val tileActivityClass: Class<out Activity>,
    context: Context,
    service: TileService,
) : ProxyTileActivityLauncher {

  private val launchMethod by
      lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          LaunchMethod.NewWay(tileActivityClass, context, service)
        } else {
          LaunchMethod.OldWay(tileActivityClass, context, service)
        }
      }

  override fun launchTileActivity() {
    launchMethod.launchTileActivity()
  }

  internal interface LaunchMethod {

    /** Open the tile activity */
    fun launchTileActivity()

    abstract class Base(
        tileActivityClass: Class<out Activity>,
        context: Context,
        private val service: TileService,
    ) : LaunchMethod {

      protected val tileActivityIntent by
          lazy(LazyThreadSafetyMode.NONE) {
            Intent(context, tileActivityClass).apply {
              flags =
                  Intent.FLAG_ACTIVITY_SINGLE_TOP or
                      Intent.FLAG_ACTIVITY_CLEAR_TOP or
                      Intent.FLAG_ACTIVITY_NEW_TASK
            }
          }

      private inline fun ensureUnlocked(crossinline block: () -> Unit) {
        if (service.isLocked) {
          service.unlockAndRun {
            Timber.d { "Unlock device before running launcher" }
            block()
          }
        } else {
          Timber.d { "Device is unlocked, run launcher" }
          block()
        }
      }

      final override fun launchTileActivity() {
        ensureUnlocked {
          Timber.d { "Start TileActivity!" }
          onLaunchTileActivity(service)
        }
      }

      protected abstract fun onLaunchTileActivity(service: TileService)
    }

    class OldWay(
        tileActivityClass: Class<out Activity>,
        context: Context,
        service: TileService,
    ) : Base(tileActivityClass, context, service) {

      @SuppressLint("StartActivityAndCollapseDeprecated")
      override fun onLaunchTileActivity(service: TileService) {
        @Suppress("DEPRECATION") service.startActivityAndCollapse(tileActivityIntent)
      }
    }

    class NewWay(
        tileActivityClass: Class<out Activity>,
        context: Context,
        service: TileService,
    ) : Base(tileActivityClass, context, service) {

      private val pendingIntent by
          lazy(LazyThreadSafetyMode.NONE) {
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_ACTIVITY,
                tileActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
          }

      @RequiresApi(34)
      override fun onLaunchTileActivity(service: TileService) {
        service.startActivityAndCollapse(pendingIntent)
      }

      companion object {
        private const val REQUEST_CODE_ACTIVITY = 42069
      }
    }
  }
}
