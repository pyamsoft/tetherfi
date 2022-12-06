package com.pyamsoft.tetherfi.service.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.tetherfi.TetherFiComponent
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null
  @Inject @JvmField internal var wiDiReceiverRegister: WiDiReceiverRegister? = null

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

    // Launch a parent scope for all jobs
    scope.launch(context = Dispatchers.Main) {

      // Register for WiDi events
      launch(context = Dispatchers.Main) { wiDiReceiverRegister.requireNotNull().register() }

      // Start notification first for Android O
      launch(context = Dispatchers.Main) {
        foregroundHandler
            .requireNotNull()
            .startProxy(
                scope = this,
                onShutdownService = { stopSelf() },
            )
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")
    notificationLauncher.requireNotNull().stop(this)

    // Grab these here so that they can be nulled safely later
    // since scope.launch is not immediate, the member may be null by the time
    // the code runs
    val handler = foregroundHandler
    val register = wiDiReceiverRegister

    // Launch a parent scope for all jobs
    scope.launch(context = Dispatchers.Main) {

      // Stop proxy
      if (handler != null) {
        launch(context = Dispatchers.Main) { handler.stopProxy(this) }
      }

      // Stop WiDi receiver
      if (register != null) {
        launch(context = Dispatchers.Main) { register.unregister() }
      }
    }

    foregroundHandler = null
    notificationLauncher = null
    wiDiReceiverRegister = null
  }
}
