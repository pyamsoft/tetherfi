package com.pyamsoft.tetherfi.service.prereq

enum class HotspotStartBlocker(val required: Boolean) {
  /** We must have permission */
  PERMISSION(required = true),

  /** In the future, the user can "agree" to "try anyway" */
  VPN(required = false)
}
