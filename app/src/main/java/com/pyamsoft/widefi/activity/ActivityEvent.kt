package com.pyamsoft.widefi.activity

import androidx.annotation.CheckResult

interface ActivityEvent {
  val host: String
  val tcpConnections: ConnectionTracker

  @CheckResult fun addTcpConnection(url: String, method: String): ActivityEvent

  companion object {

    @JvmStatic
    @CheckResult
    fun forHost(host: String): ActivityEvent {
      return ActivityEventImpl(
          host = host,
          tcpConnections = ConnectionTrackerImpl(),
      )
    }
  }
}

interface ConnectionTracker {

  @CheckResult fun get(): List<Pair<String, Map<String, Int>>>

  companion object {

    @JvmStatic
    @CheckResult
    fun empty(): ConnectionTracker {
      return ConnectionTrackerImpl()
    }
  }
}

private class ConnectionTrackerImpl() : ConnectionTracker {

  private val connections = mutableMapOf<String, MutableMap<String, Int>>()

  override fun get(): List<Pair<String, Map<String, Int>>> {
    return connections.toList()
  }

  @CheckResult
  fun add(url: String, method: String): ConnectionTrackerImpl {
    val methodMap = connections.getOrPut(url) { mutableMapOf() }
    val count = methodMap.getOrPut(method) { 0 }
    methodMap[method] = count + 1
    return this
  }
}

private data class ActivityEventImpl(
    override val host: String,
    override val tcpConnections: ConnectionTrackerImpl
) : ActivityEvent {

  @CheckResult
  override fun addTcpConnection(url: String, method: String): ActivityEvent {
    val c = tcpConnections
    return ActivityEventImpl(
        host = host,
        tcpConnections = c.add(url, method),
    )
  }
}
