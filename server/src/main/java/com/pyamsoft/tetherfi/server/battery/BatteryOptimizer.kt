package com.pyamsoft.tetherfi.server.battery

import androidx.annotation.CheckResult

interface BatteryOptimizer {

  @CheckResult suspend fun isOptimizationsIgnored(): Boolean
}
