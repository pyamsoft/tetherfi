package com.pyamsoft.widefi.server

import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.ChecksSdkIntAtLeast

object ServerDefaults {
  const val SSID = "WideFi"
  const val PORT = 8228
  val NETWORK_BAND = ServerNetworkBand.AUTO

  @JvmStatic
  @CheckResult
  fun asSsid(ssid: String): String {
    return "DIRECT-WF-${ssid}"
  }

  @CheckResult
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  fun canUseCustomConfig(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
