package com.pyamsoft.tetherfi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ServiceLauncher
@Inject
internal constructor(
    private val context: Context,
    private val foregroundServiceClass: Class<out Service>,
    private val foregroundHandler: ForegroundHandler,
) {

  private val foregroundService by
      lazy(LazyThreadSafetyMode.NONE) { Intent(context, foregroundServiceClass) }

  fun startForeground() {
    Timber.d("Start Foreground Service!")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(foregroundService)
    } else {
      context.startService(foregroundService)
    }
  }

  fun stopForeground() {
    Timber.d("Stop Foreground Service!")
    context.stopService(foregroundService)

    // Also directly stop the network
    //
    // If you get into a network error state where you never initially launched the service to begin
    // with, stuff will be perma locked.
    Timber.d("Directly calling stop on the network to avoid an Error-Lock state")
    foregroundHandler.stopProxy()
  }
}
