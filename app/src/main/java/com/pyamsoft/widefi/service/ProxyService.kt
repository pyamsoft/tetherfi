package com.pyamsoft.widefi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.widefi.WidefiComponent
import javax.inject.Inject

internal class ProxyService internal constructor() : Service() {

  @Inject @JvmField internal var launcher: NotificationLauncher? = null

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    Injector.obtainFromApplication<WidefiComponent>(application).inject(this)

    launcher.requireNotNull().start(service = this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    launcher.requireNotNull().stop(service = this)

    stopSelf()

    launcher = null
  }

  companion object {

    @JvmStatic
    fun start(context: Context) {
      val appContext = context.applicationContext
      val intent = Intent(appContext, ProxyService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    }

    @JvmStatic
    fun stop(context: Context) {
      val appContext = context.applicationContext
      val intent = Intent(appContext, ProxyService::class.java)
      appContext.stopService(intent)
    }
  }
}
