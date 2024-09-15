/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import com.pyamsoft.pydroid.util.ifNotCancellation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
internal class WifiDirectNetwork
@Inject
internal constructor(
    @ServerInternalApi private val config: WiDiConfig,
    @ServerInternalApi private val register: WifiDirectRegister,
    private val proxy: SharedProxy,
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

  @CheckResult
  private fun createChannel(): Channel? {
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
  private suspend fun connectChannel(channel: Channel) {
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
                cont.resumeWithException(RuntimeException("DEBUG: Force Fake Broadcast Error"))
              } else {
                cont.resume(Unit)
              }
            }

            override fun onFailure(reason: Int) {
              val r = WiFiDirectError.Reason.parseReason(reason)
              val e = RuntimeException("Broadcast Error: ${r.displayReason}")
              Timber.e(e) { "Unable to create Wifi Direct Group" }
              cont.resumeWithException(e)
            }
          }

      if (conf != null) {
        createGroupQ(channel, conf, listener)
      } else {
        wifiP2PManager.createGroup(
            channel,
            listener,
        )
      }
    }
  }

  @CheckResult
  private suspend fun attemptReUseConnection(channel: Channel): Boolean {
    // Sometimes, if the system has not closed down the Wifi group (because an old version of the
    // app made a group and a new one was then installed before the group was shut down) we can
    // re-use the existing group info.
    //
    // This is generally a speed win and so we take it.
    //
    // NOTE: This can in rare cases lead to the UI being out of sync, as the existing group was
    //       created with OLD name/password. The UI could have been changed and then started again.
    val result =
        withLockUpdateNetworkInfo(
            source = channel,
            force = true,
            onlyAcceptWhenAllConnected = true,
        )

    // We expect both connections to be true for this to succeed
    return result.connection && result.group
  }

  override suspend fun withLockStartBroadcast(): Channel {
    val channel = createChannel()
    if (channel == null) {
      Timber.w { "Failed to create a Wi-Fi direct channel" }
      throw RuntimeException("Unable to create Wi-Fi Direct Channel")
    }

    try {
      Timber.d { "Attempt open connection with channel" }
      if (attemptReUseConnection(channel)) {
        Timber.d { "Existing Wi-Fi group connection was re-used!" }
      } else {
        Timber.d { "Cannot re-use Wi-Fi group connection, make new one" }
        connectChannel(channel)
        Timber.d { "New Wi-Fi group connection created!" }
      }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e) { "Failed to connect Wi-Fi direct group" }
        throw e
      }
    }

    return channel
  }

  override suspend fun withLockStopBroadcast(source: Channel) {
    // This may fail if WiFi is off, but that's fine since if WiFi is off,
    // the system has already cleaned us up.
    removeGroup(source)

    // Close the wifi channel now that we are done with it
    Timber.d { "Close WiFiP2PManager channel" }
    closeSilent(source)
  }

  override suspend fun resolveCurrentConnectionInfo(
      source: Channel
  ): BroadcastNetworkStatus.ConnectionInfo {
    val info = resolveConnectionInfo(source)
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

  override suspend fun resolveCurrentGroupInfo(source: Channel): BroadcastNetworkStatus.GroupInfo {
    val group = resolveCurrentGroup(source)
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
    // Need to mark the network as running so that the Proxy network can start
    Timber.d { "Wifi Direct is fully set up!" }
    status.set(RunningStatus.Running)

    launch(context = Dispatchers.Default) { register.register() }

    launch(context = Dispatchers.Default) { inAppRatingPreferences.markHotspotUsed() }

    launch(context = Dispatchers.Default) { proxy.start(connectionStatus) }
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
