package com.pyamsoft.tetherfi.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.pyamsoft.tetherfi.service.foreground.ProxyForegroundService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, ProxyForegroundService::class.java) }

  fun startForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(foregroundService)
    } else {
      context.startService(foregroundService)
    }
  }

  fun stopForeground() {
    context.stopService(foregroundService)
  }
}
