package com.pyamsoft.tetherfi.status

sealed class PermissionRequests {

  object ToggleProxy : PermissionRequests()

  object RefreshNotificationPermission : PermissionRequests()
}
