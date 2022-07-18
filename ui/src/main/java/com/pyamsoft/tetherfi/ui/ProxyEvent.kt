package com.pyamsoft.tetherfi.ui

import androidx.annotation.CheckResult

interface ProxyEvent {
  val host: String
  val tcpConnections: ConnectionTracker

  @CheckResult fun addTcpConnection(url: String, method: String): ProxyEvent

  companion object {

    @JvmStatic
    @CheckResult
    fun forHost(host: String): ProxyEvent {
      return ProxyEventImpl(
          host = host,
          tcpConnections = ConnectionTrackerImpl(),
      )
    }
  }
}

interface ConnectionTracker {

  @CheckResult fun get(): List<Pair<String, Map<String, Int>>>
}

private class ConnectionTrackerImpl : ConnectionTracker {

  private val connections = mutableMapOf<String, MutableMap<String, Int>>()
  private var connectionList = listOf<Pair<String, MutableMap<String, Int>>>()

  override fun get(): List<Pair<String, Map<String, Int>>> {
    return connectionList
  }

  @CheckResult
  fun add(url: String, method: String): ConnectionTrackerImpl {
    val methodMap = connections.getOrPut(url) { mutableMapOf() }
    val count = methodMap.getOrPut(method) { 0 }
    methodMap[method] = count + 1
    connectionList = connections.toList()
    return this
  }
}

private data class ProxyEventImpl(
    override val host: String,
    override val tcpConnections: ConnectionTrackerImpl
) : ProxyEvent {

  @CheckResult
  override fun addTcpConnection(url: String, method: String): ProxyEvent {
    val c = tcpConnections
    return ProxyEventImpl(
        host = host,
        tcpConnections = c.add(url, method),
    )
  }
}
