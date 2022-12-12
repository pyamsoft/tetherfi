package com.pyamsoft.tetherfi.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.service.foreground.ForegroundHandler
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import timber.log.Timber

internal class ProxyForegroundService internal constructor() : Service() {

  @Inject @JvmField internal var notificationLauncher: NotificationLauncher? = null
  @Inject @JvmField internal var foregroundHandler: ForegroundHandler? = null
  @Inject @JvmField internal var wiDiReceiverRegister: WiDiReceiverRegister? = null

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ObjectGraph.ApplicationScope.retrieve(this).plusForeground().create().inject(this)

    Timber.d("Creating service")

    // Start notification first for Android O immediately
    notificationLauncher.requireNotNull().start(this)

    // Register for WiDi events
    wiDiReceiverRegister.requireNotNull().register()

    // Prepare proxy on create
    foregroundHandler
        .requireNotNull()
        .bind(
            onShutdownService = {
              Timber.d("Shutdown event received. Stopping service")
              stopSelf()
            },
        )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Constantly attempt to start proxy here instead of in onCreate
    //
    // If we spam ON/OFF, the service is created but the proxy is only started again within this
    // block.
    Timber.d("Starting Proxy!")
    foregroundHandler.requireNotNull().startProxy()
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")

    notificationLauncher?.stop(this)
    foregroundHandler?.destroy()
    wiDiReceiverRegister?.unregister()

    foregroundHandler = null
    notificationLauncher = null
    wiDiReceiverRegister = null
  }
}
