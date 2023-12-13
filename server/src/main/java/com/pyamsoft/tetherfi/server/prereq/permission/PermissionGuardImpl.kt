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

package com.pyamsoft.tetherfi.server.prereq.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PermissionGuardImpl
@Inject
internal constructor(
    private val context: Context,
) : PermissionGuard {

  @CheckResult
  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context.applicationContext, permission) ==
        PackageManager.PERMISSION_GRANTED
  }

  override val requiredPermissions: List<String> by
      lazy(LazyThreadSafetyMode.NONE) {
        // Always require these WiFi permissions
        ALWAYS_PERMISSIONS +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              listOf(
                  // For service notifications
                  android.Manifest.permission.POST_NOTIFICATIONS,
              )
            } else {
              emptyList()
            } +
            // On API < 33, we require location permission
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
              listOf(
                  android.Manifest.permission.ACCESS_COARSE_LOCATION,
                  android.Manifest.permission.ACCESS_FINE_LOCATION,
              )
            } else {
              // On API >= 33, we can use the new NEARBY_WIFI_DEVICES permission
              listOf(
                  android.Manifest.permission.NEARBY_WIFI_DEVICES,
              )
            }
      }

  override suspend fun canCreateNetwork(): Boolean =
      withContext(context = Dispatchers.Main) {
        return@withContext requiredPermissions.all { hasPermission(it) }
      }

  companion object {

    private val ALWAYS_PERMISSIONS =
        listOf(
            // To open Wifi direct network
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
        )
  }
}
