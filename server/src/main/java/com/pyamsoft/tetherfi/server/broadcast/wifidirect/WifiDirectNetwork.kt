/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.broadcast.wifidirect

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetwork
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastServer
import com.pyamsoft.tetherfi.server.broadcast.BroadcastStatus
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.status.RunningStatus
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Singleton
internal class WifiDirectNetwork
@Inject
internal constructor(
    @ServerInternalApi private val proxy: SharedProxy,
    @ServerInternalApi private val config: WiDiConfig,
    @ServerInternalApi private val register: WifiDirectRegister,
    private val inAppRatingPreferences: InAppRatingPreferences,
    private val appContext: Context,
    private val appEnvironment: AppDevEnvironment,
    private val enforcer: ThreadEnforcer,
    shutdownBus: EventBus<ServerShutdownEvent>,
    permissionGuard: PermissionGuard,
    clock: Clock,
    status: BroadcastStatus,
) :
    BroadcastServer<Channel>(
        shutdownBus,
        permissionGuard,
        appEnvironment,
        enforcer,
        clock,
        status,
    ),
    BroadcastNetwork,
    BroadcastNetworkStatus {

  private val wifiP2PManager by lazy {
    appContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  override val canReUseDataSourceConnection: Boolean = true

  @SuppressLint("MissingPermission")
  private fun createGroupQ(
      channel: Channel,
      config: WifiP2pConfig,
      listener: WifiP2pManager.ActionListener,
  ) {
    if (ServerDefaults.canUseCustomConfig()) {
      wifiP2PManager.createGroup(
          channel,
          config,
          listener,
      )
    } else {
      throw IllegalStateException("Called createGroupQ but not Q: ${Build.VERSION.SDK_INT}")
    }
  }

  @CheckResult
  private suspend fun removeGroup(channel: Channel) {
    enforcer.assertOffMainThread()

    Timber.d { "Stop existing WiFi Group" }
    return suspendCoroutine { cont ->
      wifiP2PManager.removeGroup(
          channel,
          object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
              Timber.d { "Wifi P2P Channel is removed" }
              cont.resume(Unit)
            }

            override fun onFailure(reason: Int) {
              val r = WiFiDirectError.Reason.parseReason(reason)
              Timber.w { "Failed to stop network: ${r.displayReason}" }

              Timber.d { "Close Group failed but continue teardown anyway" }
              cont.resume(Unit)
            }
          },
      )
    }
  }

  @CheckResult
  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(channel: Channel): WifiP2pGroup? {
    enforcer.assertOffMainThread()

    return suspendCoroutine { cont ->
      try {
        wifiP2PManager.requestGroupInfo(channel) {
          // We are still on the Main Thread here, so don't unpack anything yet.
          cont.resume(it)
        }
      } catch (e: Throwable) {
        Timber.e(e) { "Error getting WiFi Direct Group Info" }
        cont.resumeWithException(e)
      }
    }
  }

  @CheckResult
  private suspend fun resolveConnectionInfo(channel: Channel): WifiP2pInfo? {
    enforcer.assertOffMainThread()

    return suspendCoroutine { cont ->
      try {
        wifiP2PManager.requestConnectionInfo(channel) {
          // We are still on the Main Thread here, so don't unpack anything yet.
          cont.resume(it)
        }
      } catch (e: Throwable) {
        Timber.e(e) { "Error getting WiFi Direct Connection Info" }
        cont.resumeWithException(e)
      }
    }
  }

  override suspend fun createDataSource(): Channel? {
    enforcer.assertOffMainThread()

    Timber.d { "Creating WifiP2PManager Channel" }

    // This can return null if initialization fails
    return wifiP2PManager.initialize(
        appContext,
        Looper.getMainLooper(),
    ) {
      // Before we used to kill the Network
      //
      // But now we do nothing - if you Swipe Away the app from recents,
      // the p2p manager will die, but when it comes back we want everything to
      // attempt to run again so we leave this around.
      //
      // Any other unexpected death like Airplane mode or Wifi off should be covered by the receiver
      // so we should never unintentionally leak the service
      Timber.d { "WifiP2PManager Channel died! Do nothing :D" }
    }
  }

  @SuppressLint("MissingPermission")
  override suspend fun connectDataSource(dataSource: Channel): RunningStatus {
    enforcer.assertOffMainThread()

    Timber.d { "Creating new wifi p2p group" }
    val conf = config.getConfiguration()

    return suspendCoroutine { cont ->
      val listener =
          object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
              Timber.d { "New network created" }

              val fakeError = appEnvironment.isBroadcastFakeError
              if (fakeError.value) {
                Timber.w { "DEBUG forcing Fake Broadcast Error" }
                cont.resume(
                    WiFiDirectError(
                        WiFiDirectError.Reason.Unknown(-1),
                        RuntimeException("DEBUG: Force Fake Broadcast Error"),
                    ),
                )
              } else {
                cont.resume(RunningStatus.Running)
              }
            }

            override fun onFailure(reason: Int) {
              val r = WiFiDirectError.Reason.parseReason(reason)
              val e = RuntimeException("Broadcast Error: ${r.displayReason}")
              Timber.e(e) { "Unable to create Wifi Direct Group" }
              cont.resume(WiFiDirectError(r, e))
            }
          }

      if (conf != null) {
        createGroupQ(dataSource, conf, listener)
      } else {
        wifiP2PManager.createGroup(
            dataSource,
            listener,
        )
      }
    }
  }

  override suspend fun disconnectDataSource(dataSource: Channel) {
    // This may fail if WiFi is off, but that's fine since if WiFi is off,
    // the system has already cleaned us up.
    removeGroup(dataSource)

    // Close the wifi channel now that we are done with it
    Timber.d { "Close WiFiP2PManager channel" }
    closeSilent(dataSource)
  }

  override suspend fun resolveCurrentConnectionInfo(
      dataSource: Channel
  ): BroadcastNetworkStatus.ConnectionInfo {
    val info = resolveConnectionInfo(dataSource)
    val host = info?.groupOwnerAddress
    return if (host == null) {
      BroadcastNetworkStatus.ConnectionInfo.Error(
          error = IllegalStateException("WiFi Direct did not return Connection Info"),
      )
    } else {
      BroadcastNetworkStatus.ConnectionInfo.Connected(
          hostName = host.hostName.orEmpty(),
      )
    }
  }

  override suspend fun resolveCurrentGroupInfo(
      dataSource: Channel
  ): BroadcastNetworkStatus.GroupInfo {
    val group = resolveCurrentGroup(dataSource)
    return if (group == null) {
      BroadcastNetworkStatus.GroupInfo.Error(
          error = IllegalStateException("WiFi Direct did not return Group Info"),
      )
    } else {
      BroadcastNetworkStatus.GroupInfo.Connected(
          ssid = group.networkName,
          password = group.passphrase,
      )
    }
  }

  override fun CoroutineScope.onNetworkStarted(
      connectionStatus: Flow<BroadcastNetworkStatus.ConnectionInfo>
  ) {
    // Once the proxy is marked "starting", then the Wifi-Direct side is done
    onProxyStatusChanged()
        .filter { it is RunningStatus.Starting }
        .also { f ->
          launch(context = Dispatchers.Default) {
            f.collect {
              Timber.d { "Wifi Direct is fully set up!" }
              status.set(RunningStatus.Running)
            }
          }
        }

    launch(context = Dispatchers.Default) { register.register() }

    launch(context = Dispatchers.Default) { proxy.start(connectionStatus) }

    launch(context = Dispatchers.Default) { inAppRatingPreferences.markHotspotUsed() }
  }

  override fun CoroutineScope.onNetworkStopped(clearErrorStatus: Boolean) {}

  override fun onProxyStatusChanged(): Flow<RunningStatus> {
    return proxy.onStatusChanged()
  }

  override fun getCurrentProxyStatus(): RunningStatus {
    return proxy.getCurrentStatus()
  }

  companion object {

    @JvmStatic
    private fun closeSilent(s: Channel) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        try {
          s.close()
        } catch (e: Throwable) {
          Timber.e(e) { "Failed to close WifiP2P Channel" }
        }
      }
    }
  }
}
