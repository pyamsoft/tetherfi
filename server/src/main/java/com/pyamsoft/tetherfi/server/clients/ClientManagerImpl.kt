package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ClientManagerImpl @Inject internal constructor() :
    BlockedClientTracker, BlockedClients, SeenClients {

  private val blockedClients = MutableStateFlow<MutableSet<TetherClient>>(mutableSetOf())
  private val seenClients = MutableStateFlow<MutableSet<TetherClient>>(mutableSetOf())

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

  override suspend fun block(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        blockedClients.update { clients ->
          val existing = clients.firstOrNull { isMatchingClient(it, client) }
          clients.apply {
            if (existing == null) {
              clients.add(client)
            }
          }
        }
      }

  override suspend fun unblock(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        blockedClients.update { clients ->
          clients.apply { clients.removeIf { isMatchingClient(it, client) } }
        }
      }

  override fun listenForBlocked(): Flow<Set<TetherClient>> {
    return blockedClients
  }

  override suspend fun isBlocked(client: TetherClient): Boolean =
      withContext(context = Dispatchers.Default) {
        val blocked = listenForBlocked().first()
        return@withContext blocked.firstOrNull { isMatchingClient(it, client) } != null
      }

  override suspend fun seen(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        seenClients.update { clients ->
          val existing = clients.firstOrNull { isMatchingClient(it, client) }
          clients.apply {
            if (existing == null) {
              clients.add(client)
            }
          }
        }
      }

  override suspend fun clear() =
      withContext(context = Dispatchers.Default) { seenClients.update { it.apply { clear() } } }

  override fun listenForClients(): Flow<Set<TetherClient>> {
    return seenClients
  }
}
