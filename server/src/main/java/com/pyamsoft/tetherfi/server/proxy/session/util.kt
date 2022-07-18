package com.pyamsoft.tetherfi.server.proxy.session

import android.net.TrafficStats

internal fun tagSocket() {
  // On Android O and above, StrictMode causes untagged socket errors
  // Setting the ThreadStatsTag seems to fix it
  TrafficStats.setThreadStatsTag(1)
}
