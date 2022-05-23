package com.pyamsoft.tetherfi.server.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

  override fun canCreateWiDiNetwork(): Boolean {
    return ALL_PERMISSIONS.all { hasPermission(it) }
  }

  override fun requiredPermissions(): List<String> {
    return ALL_PERMISSIONS
  }

  companion object {

    private val ALL_PERMISSIONS =
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
        )
  }
}
