package com.pyamsoft.tetherfi.server.permission

import androidx.annotation.CheckResult

interface PermissionGuard {

  @get:CheckResult val requiredPermissions: List<String>

  @CheckResult fun canCreateWiDiNetwork(): Boolean
}
