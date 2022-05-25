package com.pyamsoft.tetherfi.server

import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.ChecksSdkIntAtLeast

object ServerDefaults {
  const val SSID = "TetherFi"
  const val PORT = 8228
  val NETWORK_BAND = ServerNetworkBand.AUTO

  @JvmStatic
  @CheckResult
  fun asSsid(ssid: String): String {
    return "DIRECT-TF-${ssid}"
  }

  @CheckResult
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  fun canUseCustomConfig(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
