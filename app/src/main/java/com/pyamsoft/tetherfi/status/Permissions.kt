package com.pyamsoft.tetherfi.status

sealed class PermissionRequests {

  object Server : PermissionRequests()

  object Notification : PermissionRequests()
}

sealed class PermissionResponse {

  object ToggleProxy : PermissionResponse()

  object RefreshNotification : PermissionResponse()
}
