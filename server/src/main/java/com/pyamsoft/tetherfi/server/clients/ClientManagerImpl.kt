package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ClientManagerImpl @Inject internal constructor() :
    BlockedClientTracker, BlockedClients, SeenClients, ClientEraser {

  private val blockedClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())
  private val seenClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())

  @CheckResult
  private fun isMatchingClient(c1: TetherClient, c2: TetherClient): Boolean {
    when (c1) {
      is TetherClient.IpAddress -> {
        if (c2 is TetherClient.IpAddress) {
          return c1.ip == c2.ip
        }

        return false
      }
      is TetherClient.HostName -> {
        if (c2 is TetherClient.HostName) {
          return c1.hostname == c2.hostname
        }

        return false
      }
    }
  }

  @CheckResult
  private fun addToSet(set: Set<TetherClient>, client: TetherClient): Set<TetherClient> {
    val existing = set.firstOrNull { isMatchingClient(it, client) }
    return set.run {
      if (existing == null) {
        this + client
      } else {
        this
      }
    }
  }

  override fun block(client: TetherClient) {
    blockedClients.update { addToSet(it, client) }
  }

  override fun unblock(client: TetherClient) {
    blockedClients.update { clients ->
      val existing = clients.firstOrNull { isMatchingClient(it, client) }
      return@update clients.run {
        if (existing == null) {
          this
        } else {
          this - existing
        }
      }
    }
  }

  override fun isBlocked(client: TetherClient): Boolean {
    val blocked = blockedClients.value
    return blocked.firstOrNull { isMatchingClient(it, client) } != null
  }

  override fun seen(client: TetherClient) {
    seenClients.update { addToSet(it, client) }
  }

  override fun clear() {
    seenClients.value = emptySet()
    blockedClients.value = emptySet()
  }

  override fun listenForClients(): Flow<Set<TetherClient>> {
    return seenClients
  }

  override fun listenForBlocked(): Flow<Set<TetherClient>> {
    return blockedClients
  }
}
