package com.pyamsoft.tetherfi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.tetherfi.TetherFiComponent
import com.pyamsoft.tetherfi.server.event.OnShutdownEvent
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.lock.Locker
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProxyService internal constructor() : Service() {

  @Inject @JvmField internal var shutdownBus: EventConsumer<OnShutdownEvent>? = null

  @Inject @JvmField internal var launcher: NotificationLauncher? = null
  @Inject @JvmField internal var locker: Locker? = null

  @Inject @JvmField internal var network: WiDiNetwork? = null
  @Inject @JvmField internal var status: WiDiNetworkStatus? = null

  private val scope = MainScope()

  private var shutdownJob: Job? = null
  private var statusJob: Job? = null

  private suspend fun acquireCpuWakeLock() {
    Timber.d("Attempt to claim CPU wakelock")
    locker.requireNotNull().acquire()
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    Injector.obtainFromApplication<TetherFiComponent>(application).inject(this)

    // Start notification first for Android O
    launcher.requireNotNull().start(service = this)

    // Listen to shutdown command
    shutdownJob?.cancel()
    shutdownJob =
        scope.launch(context = Dispatchers.Main) {
          shutdownBus.requireNotNull().onEvent {
            Timber.d("Shutdown event received!")
            stopSelf()
          }
        }

    // Watch status of network
    statusJob?.cancel()
    statusJob =
        scope.launch(context = Dispatchers.Main) {
          status.requireNotNull().onStatusChanged { s ->
            when (s) {
              is RunningStatus.Running -> {
                Timber.d("WiDi Network started!")
                // Acquire wake lock
                acquireCpuWakeLock()
              }
              else -> Timber.d("Server status changed: $s")
            }
          }
        }

    // Start network
    scope.launch(context = Dispatchers.Main) {
      Timber.d("Start WiDi Network")
      network.requireNotNull().start()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Destroying service")

    // Local reference network here in case this coroutine launches after network is nulled out.
    val widi = network
    if (widi != null) {
      scope.launch(context = Dispatchers.Main) {
        Timber.d("Stop WiDi network")
        widi.stop()
      }
    }

    // Local reference locker here in case this coroutine launches after locker is nulled out.
    val lock = locker
    if (lock != null) {
      scope.launch(context = Dispatchers.Main) {
        Timber.d("Attempt to release CPU wakelock")
        lock.release()
      }
    }

    // Stop notification
    launcher?.stop(service = this)

    // Cancel jobs
    shutdownJob?.cancel()
    shutdownJob = null

    statusJob?.cancel()
    statusJob = null

    // Clear
    shutdownBus = null
    launcher = null
    network = null
    locker = null
    status = null
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
