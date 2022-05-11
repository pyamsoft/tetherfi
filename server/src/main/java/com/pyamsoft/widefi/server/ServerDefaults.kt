package com.pyamsoft.widefi.server

import androidx.annotation.CheckResult

object ServerDefaults {
  const val SSID = "WideFi"
  const val PASSWORD = "WideFi69"
  const val PORT = 8228
  val NETWORK_BAND = ServerNetworkBand.AUTO


  @JvmStatic
  @CheckResult
  fun asSsid(ssid: String): String {
    return "DIRECT-WF-${ssid}"
  }
}
