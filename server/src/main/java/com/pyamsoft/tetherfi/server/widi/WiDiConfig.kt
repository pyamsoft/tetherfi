package com.pyamsoft.tetherfi.server.widi

import android.net.wifi.p2p.WifiP2pConfig
import androidx.annotation.CheckResult

internal interface WiDiConfig {

  @CheckResult suspend fun getConfiguration(): WifiP2pConfig?
}
