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
import android.net.Network
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import io.ktor.network.selector.Selectable
import io.ktor.network.sockets.Socket
import java.nio.channels.SocketChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidNetworkBinder @Inject internal constructor(
    private val context: Context,
    private val enforcer: ThreadEnforcer,
) : NetworkBinder {

    private val connectivityManager by lazy {
        enforcer.assertOffMainThread()
        context.getSystemService<ConnectivityManager>().requireNotNull()
    }

    @CheckResult
    private fun getPreferredNetwork(): Network? {
        // TODO get preferred network
        return connectivityManager.activeNetwork
    }

    private fun bindSocketToNetwork(socket: Socket, network: Network) {
        if (socket is Selectable) {
            val channel = socket.channel
            if (channel is SocketChannel) {
                Timber.d { "Bind socket to network $channel -> $network" }
                network.bindSocket(channel.socket())
            } else {
                Timber.w { "Cannot attempt bindSocket - Channel is not SocketChannel: $channel" }
            }
        } else {
            Timber.w { "Cannot attempt bindSocket - Socket is not selectable: $socket" }
        }
    }

    override suspend fun bindToNetwork(socket: Socket) {
        // https://github.com/pyamsoft/tetherfi/issues/154
        // https://github.com/pyamsoft/tetherfi/issues/331
        if (IS_SOCKET_BIND_ENABLED) {
            val network = getPreferredNetwork()
            if (network != null) {
                bindSocketToNetwork(
                    socket = socket, network = network,
                )
            }
        }
    }

    companion object {
        // KTOR needs to support
        // https://youtrack.jetbrains.com/issue/KTOR-7452/Question-KTOR-Socket-using-Android-bindSocketgg
        private const val IS_SOCKET_BIND_ENABLED = false
    }
}