package com.pyamsoft.tetherfi.status.vpn

import androidx.annotation.CheckResult

internal interface VpnChecker {

  @CheckResult fun isUsingVpn(): Boolean
}
