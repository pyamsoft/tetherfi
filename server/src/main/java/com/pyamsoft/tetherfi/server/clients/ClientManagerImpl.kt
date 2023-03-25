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
    BlockedClientTracker, BlockedClients {

  private val blockedClients = MutableStateFlow<MutableSet<TetherClient>>(mutableSetOf())

  override suspend fun block(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        blockedClients.update { it.apply { add(client) } }
      }

  override suspend fun unblock(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        blockedClients.update { it.apply { remove(client) } }
      }

  override fun listenForChanges(): Flow<Set<TetherClient>> {
    return blockedClients
  }

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

  override suspend fun isBlocked(client: TetherClient): Boolean =
      withContext(context = Dispatchers.Default) {
        val blocked = listenForChanges().first()
        return@withContext blocked.firstOrNull { isMatchingClient(it, client) } != null
      }
}
