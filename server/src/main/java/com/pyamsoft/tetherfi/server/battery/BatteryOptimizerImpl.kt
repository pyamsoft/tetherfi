package com.pyamsoft.tetherfi.server.battery

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BatteryOptimizerImpl
@Inject
internal constructor(
    private val context: Context,
) : BatteryOptimizer {

  private val powerManager by lazy { context.getSystemService<PowerManager>().requireNotNull() }

  override suspend fun isOptimizationsIgnored(): Boolean {
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }
}
