package com.pyamsoft.tetherfi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
    private val foregroundServiceClass: Class<out Service>,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, foregroundServiceClass) }

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
