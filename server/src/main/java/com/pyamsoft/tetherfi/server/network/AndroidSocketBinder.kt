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

package com.pyamsoft.tetherfi.server.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import io.ktor.network.selector.Selectable
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.nio.channels.SocketChannel
import javax.inject.Inject
import javax.inject.Singleton

// https://github.com/pyamsoft/tetherfi/issues/154
// https://github.com/pyamsoft/tetherfi/issues/331
@Singleton
internal class AndroidSocketBinder
@Inject
internal constructor(
    private val context: Context,
    private val enforcer: ThreadEnforcer,
) : SocketBinder, SocketBinder.NetworkBinder {

  private val connectivityManager by lazy {
    enforcer.assertOffMainThread()
    context.getSystemService<ConnectivityManager>().requireNotNull()
  }

  private val mobileDataNetwork = MutableStateFlow<Network?>(null)

  @CheckResult
  private fun createMobileDataNetworkCallback(): NetworkCallback {
    return object : NetworkCallback() {
      override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Timber.d { "Mobile Data network available: $network" }
        mobileDataNetwork.value = network
      }

      override fun onUnavailable() {
        super.onUnavailable()
        Timber.w { "Mobile Data network not available!" }
        mobileDataNetwork.value = null
      }
    }
  }

  /**
   * God this was hard to find
   *
   * https://stackoverflow.com/questions/73413465/android-transport-cellular-network-not-available-if-wifi-is-connected-how-do-we
   * https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,%20android.net.ConnectivityManager.NetworkCallback)
   */
  private fun requestMobileDataNetwork(callback: NetworkCallback) {
    val request =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
    connectivityManager.requestNetwork(request, callback)
  }

  @CheckResult
  private suspend fun isBindSocketToMobileEnabled(): Boolean {
    // TODO preference
    return IS_SOCKET_BIND_ENABLED
  }

  override suspend fun withMobileDataNetworkActive(
      block: suspend (SocketBinder.NetworkBinder) -> Unit
  ) {
    if (isBindSocketToMobileEnabled()) {
      val callback = createMobileDataNetworkCallback()
      try {
        // Request first to keep the network alive
        Timber.d { "Open MobileData network for binding sockets..." }
        requestMobileDataNetwork(callback)

        // We are the bound network now
        block(this)
      } finally {
        // Once done we unregister
        Timber.d { "Close mobile data network" }
        connectivityManager.unregisterNetworkCallback(callback)
      }
    } else {
      // User does not want to bind, do absolutely nothing
      block(NOOP_BOUND)
    }
  }

  private fun bindToNetwork(channel: SocketChannel, network: Network) {
    try {
      network.bindSocket(channel.socket())
    } catch (e: Throwable) {
      Timber.e(e) { "Error binding socket to network $network, continue anyway!" }
    }
  }

  override suspend fun bindToNetwork(socket: Socket) {
    if (socket is Selectable) {
      val channel = socket.channel
      if (channel is SocketChannel) {
        val network = mobileDataNetwork.first()
        if (network != null) {
          bindToNetwork(channel, network)
        }
      } else {
        Timber.w { "Cannot attempt bindSocket - Channel is not SocketChannel: $channel" }
      }
    } else {
      Timber.w { "Cannot attempt bindSocket - Socket is not selectable: $socket" }
    }
  }

  companion object {
    // KTOR needs to support upstream
    // https://youtrack.jetbrains.com/issue/KTOR-7452/Question-KTOR-Socket-using-Android-bindSocketgg
    //
    // For now, we support using our own custom hacked build of ktor
    // https://github.com/pyamsoft/ktor
    private const val IS_SOCKET_BIND_ENABLED = true

    private val NOOP_BOUND: SocketBinder.NetworkBinder =
        SocketBinder.NetworkBinder { Timber.d { "Not binding to any socket" } }
  }
}
