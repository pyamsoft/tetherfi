package com.pyamsoft.widefi.server.widi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.widefi.server.BaseServer
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.permission.PermissionGuard
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WifiDirectWiDiNetwork
@Inject
internal constructor(
    private val context: Context,
    private val permissionGuard: PermissionGuard,
    private val proxy: SharedProxy,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork {

  private val directManager by lazy {
    context.applicationContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  private val mutex = Mutex()
  private var wifiChannel: WifiP2pManager.Channel? = null

  @CheckResult
  private fun createChannel(): WifiP2pManager.Channel? {
    return directManager.initialize(context.applicationContext, Looper.getMainLooper(), null)
  }

  @CheckResult
  private suspend fun getChannel(): WifiP2pManager.Channel? {
    return mutex.withLock { wifiChannel }
  }

  @SuppressLint("MissingPermission")
  private suspend fun createGroup(channel: WifiP2pManager.Channel): RunningStatus =
      withContext(context = Dispatchers.Main) {
        Timber.d("Creating new wifi p2p group")

        return@withContext suspendCoroutine { cont ->
          directManager.createGroup(
              channel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("New network created")
                  cont.resume(RunningStatus.Running)
                }

                override fun onFailure(reason: Int) {
                  val msg = "Failed to create network: ${reasonToString(reason)}"
                  Timber.w(msg)
                  cont.resume(RunningStatus.Error(msg))
                }
              },
          )
        }
      }

  @CheckResult
  private suspend fun removeGroup(channel: WifiP2pManager.Channel): RunningStatus =
      withContext(context = Dispatchers.Main) {
        Timber.d("Stop existing network")

        return@withContext suspendCoroutine { cont ->
          directManager.removeGroup(
              channel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("Wifi P2P Channel is removed")
                  closeSilent(channel)
                  cont.resume(RunningStatus.NotRunning)
                }

                override fun onFailure(reason: Int) {
                  val msg = "Failed to stop network: ${reasonToString(reason)}"
                  Timber.w(msg)
                  cont.resume(RunningStatus.Error(msg))
                }
              },
          )
        }
      }

  private suspend fun startNetwork() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions for making WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        val channel = createChannel()
        if (channel == null) {
          Timber.w("Failed to create channel, cannot initialize WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Start new network")
        status.set(RunningStatus.Starting)

        val runningStatus = createGroup(channel)
        if (runningStatus == RunningStatus.Running) {
          mutex.withLock { wifiChannel = channel }

          launch(context = Dispatchers.IO) {
            Timber.d("Network started, start proxy")
            proxy.start()
          }
        } else {
          launch(context = Dispatchers.IO) {
            Timber.w("Group failed creation, stop proxy")
            proxy.stop()
          }
        }
        status.set(runningStatus)
      }

  private suspend fun stopNetwork() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        val channel = getChannel()

        if (channel == null) {
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Shutting down wifi network")
        status.set(RunningStatus.Stopping)

        val runningStatus = removeGroup(channel)
        if (runningStatus == RunningStatus.NotRunning) {
          mutex.withLock { wifiChannel = null }
          launch(context = Dispatchers.IO) {
            Timber.d("Stop proxy when wifi network removed")
            proxy.stop()
          }
        } else {
          launch(context = Dispatchers.IO) {
            Timber.w("Group failed removal, stop proxy anyway")
            proxy.stop()
          }
        }
        status.set(runningStatus)
      }

  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(channel: WifiP2pManager.Channel): WiDiNetwork.GroupInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          Timber.d("Get current group info")

          directManager.requestGroupInfo(
              channel,
          ) { group ->
            if (group == null) {
              Timber.w("Wifi group info was null from P2PManager")
              cont.resume(null)
            } else {
              cont.resume(
                  WiDiNetwork.GroupInfo(
                      ssid = group.networkName,
                      password = group.passphrase,
                  ),
              )
            }
          }
        }
      }

  override suspend fun onErrorEvent(block: (ErrorEvent) -> Unit) {
    return proxy.onErrorEvent(block)
  }

  override suspend fun getGroupInfo(): WiDiNetwork.GroupInfo? =
      withContext(context = Dispatchers.IO) {
        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions for making WiDi network")
          return@withContext null
        }

        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get group info without Wifi channel")
          return@withContext null
        }

        return@withContext resolveCurrentGroup(channel)
      }

  override suspend fun start() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        stopNetwork()
        startNetwork()
      }

  override suspend fun stop() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        stopNetwork()
      }

  override suspend fun onProxyStatusChanged(block: (RunningStatus) -> Unit) {
    return proxy.onStatusChanged(block)
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun reasonToString(reason: Int): String {
      return when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
        WifiP2pManager.NO_SERVICE_REQUESTS -> "No Service Requests"
        WifiP2pManager.ERROR -> "Error"
        WifiP2pManager.BUSY -> "Busy"
        else -> "Unknown"
      }
    }

    @JvmStatic
    private fun closeSilent(s: WifiP2pManager.Channel) {
      try {
        s.close()
      } catch (e: Throwable) {
        Timber.e(e, "Failed to close WifiP2P Channel")
      }
    }
  }
}
