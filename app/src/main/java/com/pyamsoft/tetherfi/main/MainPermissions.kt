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

package com.pyamsoft.tetherfi.main

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.pydroid.util.doOnDestroy
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainPermissions
@Inject
internal constructor(
    private val permissionRequestBus: EventConsumer<PermissionRequests>,
    private val permissionResponseBus: EventBus<PermissionResponse>,
    @Named("server") private val serverPermissionRequester: PermissionRequester,
    @Named("notification") private val notificationPermissionRequester: PermissionRequester,
) {

  private var serverRequester: PermissionRequester.Requester? = null
  private var notificationRequester: PermissionRequester.Requester? = null

  fun register(activity: ComponentActivity) {
    // Watch lifecycle
    activity.doOnDestroy { unregister() }

    // Unregister any existing (double call?)
    unregister()

    val sr =
        serverPermissionRequester
            .registerRequester(activity) { granted ->
              if (granted) {
                Timber.d { "Network permission granted, toggle proxy" }

                // Broadcast in the background
                activity.lifecycleScope.launch(context = Dispatchers.Default) {
                  permissionResponseBus.emit(PermissionResponse.ToggleProxy)
                }
              } else {
                Timber.w { "Network permission not granted" }
              }
            }
            .also { serverRequester = it }

    val nr =
        notificationPermissionRequester
            .registerRequester(activity) { granted ->
              if (granted) {
                Timber.d { "Notification permission granted" }

                // Broadcast in the background
                activity.lifecycleScope.launch(context = Dispatchers.Default) {
                  permissionResponseBus.emit(PermissionResponse.RefreshNotification)
                }
              } else {
                Timber.w { "Notification permission not granted" }
              }
            }
            .also { notificationRequester = it }

    permissionRequestBus.also { f ->
      activity.lifecycleScope.launch(context = Dispatchers.Default) {
        f.collect { req ->
          when (req) {
            is PermissionRequests.Notification -> {
              nr.requestPermissions()
            }
            is PermissionRequests.Server -> {
              sr.requestPermissions()
            }
          }
        }
      }
    }
  }

  fun unregister() {
    serverRequester?.unregister()
    notificationRequester?.unregister()
  }
}
