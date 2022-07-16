package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import io.ktor.network.sockets.ConnectedDatagramSocket
import java.io.Closeable
import kotlinx.coroutines.DisposableHandle

internal interface ConnectionTracker {

  suspend fun use(info: DestinationInfo, block: suspend (instance: ConnectedDatagramSocket) -> Unit)
}

internal interface ManagedConnectionTracker : ConnectionTracker, Closeable, DisposableHandle
