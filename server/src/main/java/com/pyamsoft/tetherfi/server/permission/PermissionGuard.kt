package com.pyamsoft.tetherfi.server.permission

import androidx.annotation.CheckResult

interface PermissionGuard {

  @CheckResult fun canCreateWiDiNetwork(): Boolean

  @CheckResult fun requiredPermissions(): List<String>
}
