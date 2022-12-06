package com.pyamsoft.tetherfi.service.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.tetherfi.TetherFiComponent
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null

  private val scope by lazy(LazyThreadSafetyMode.NONE) { MainScope() }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    Injector.obtainFromApplication<TetherFiComponent>(application).inject(this)

    Timber.d("Creating service")

    // Start notification first for Android O immediately
    notificationLauncher.requireNotNull().start(this)

    // Start notification first for Android O
    scope.launch(context = Dispatchers.Main) {
      foregroundHandler
          .requireNotNull()
          .startProxy(
              scope = this,
              onShutdownService = { stopSelf() },
          )
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")
    notificationLauncher.requireNotNull().stop(this)

    foregroundHandler?.also { handler ->
      scope.launch(context = Dispatchers.Main) { handler.stopProxy(this) }
    }

    foregroundHandler = null
    notificationLauncher = null
  }
}
