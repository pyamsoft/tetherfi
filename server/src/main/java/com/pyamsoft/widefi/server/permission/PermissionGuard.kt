package com.pyamsoft.widefi.server.permission

import androidx.annotation.CheckResult

interface PermissionGuard {

  @CheckResult fun canCreateWiDiNetwork(): Boolean
}
