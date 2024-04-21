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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.doOnCreate
import com.pyamsoft.pydroid.util.doOnDestroy
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class PermissionManager internal constructor() {

  @Inject @JvmField internal var permissionRequestBus: EventConsumer<PermissionRequests>? = null

  @Inject @JvmField internal var permissionResponseBus: EventBus<PermissionResponse>? = null

  @Inject @JvmField internal var permissionGuard: PermissionGuard? = null

  @CheckResult
  private fun createServerPermissionRequester(
      activity: ComponentActivity
  ): ActivityResultLauncher<Array<String>> {
    return activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { results ->
          val ungrantedPermissions = results.filterNot { it.value }.map { it.key }
          val allPermissionsGranted = ungrantedPermissions.isEmpty()
          if (allPermissionsGranted) {
            Timber.d { "All permissions granted" }

            // Broadcast in the background
            activity.lifecycleScope.launch(context = Dispatchers.Default) {
              Timber.d { "Toggle Proxy service!" }
              permissionResponseBus.requireNotNull().emit(PermissionResponse.ToggleProxy)
            }
          } else {
            Timber.w { "Did not grant all permissions: $ungrantedPermissions" }
          }
        }
  }

  @CheckResult
  private fun createNotificationPermissionRequester(
      activity: ComponentActivity
  ): ActivityResultLauncher<String> {
    return activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted
      ->
      if (granted) {
        Timber.d { "Granted notification permission!" }

        // Broadcast in the background
        activity.lifecycleScope.launch(context = Dispatchers.Default) {
          permissionResponseBus.requireNotNull().emit(PermissionResponse.RefreshNotification)
        }
      } else {
        Timber.w { "Did not grant notification permission" }
      }
    }
  }

  private fun registerPermissionListeners(
      activity: ComponentActivity,
      serverPermissionRequester: ActivityResultLauncher<Array<String>>,
      notificationPermissionRequester: ActivityResultLauncher<String>
  ) {
    permissionRequestBus.requireNotNull().also { f ->
      activity.lifecycleScope.launch(context = Dispatchers.Default) {
        f.collect { req ->
          when (req) {
            is PermissionRequests.Notification -> {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionRequester.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS)
              } else {
                Timber.w { "Notification permission not needed old API: ${Build.VERSION.SDK_INT}" }
              }
            }
            is PermissionRequests.Server -> {
              serverPermissionRequester.launch(
                  permissionGuard.requireNotNull().requiredPermissions.toTypedArray(),
              )
            }
          }
        }
      }
    }
  }

  private fun handleDestroy() {
    permissionGuard = null
    permissionRequestBus = null
    permissionResponseBus = null
  }

  fun create(activity: ComponentActivity, component: MainComponent) {
    val serverPermissionRequester = createServerPermissionRequester(activity)
    val notificationPermissionRequester = createNotificationPermissionRequester(activity)

    component.inject(this)

    activity.doOnCreate {
      registerPermissionListeners(
          activity = activity,
          serverPermissionRequester = serverPermissionRequester,
          notificationPermissionRequester = notificationPermissionRequester,
      )
    }

    activity.doOnDestroy {
      serverPermissionRequester.unregister()
      notificationPermissionRequester.unregister()

      handleDestroy()
    }
  }
}

internal fun ComponentActivity.registerPermissionManager(component: MainComponent) {
  PermissionManager()
      .create(
          activity = this,
          component = component,
      )
}
