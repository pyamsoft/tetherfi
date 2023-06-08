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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.cancelChildren
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import timber.log.Timber
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
internal class ClientManagerImpl
@Inject
internal constructor(
    private val inAppRatingPreferences: InAppRatingPreferences,
    private val clock: Clock,
) : BlockedClientTracker, BlockedClients, SeenClients, ClientEraser {

  private val blockedClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())
  private val seenClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())

  private var watchJob: Job? = null

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.IO + CoroutineName(this::class.java.name),
    )
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

  @CheckResult
  private fun markLastSeenNow(client: TetherClient): TetherClient {
    return when (client) {
      is TetherClient.IpAddress -> client.copy(mostRecentlySeen = LocalDateTime.now(clock))
      is TetherClient.HostName -> client.copy(mostRecentlySeen = LocalDateTime.now(clock))
    }
  }

  private fun onNewClientSeen(client: TetherClient) {
    Timber.d("First time seeing client: $client")
    inAppRatingPreferences.markDeviceConnected()

    startWatchingForOldClients()
  }

  private fun startWatchingForOldClients() {
    watchJob =
        watchJob
            ?: scope.launch(context = Dispatchers.IO) {
              while (isActive) {
                val beforeWait = LocalDateTime.now(clock)

                // We are not as important
                yield()

                // Wait for 5 minutes
                delay(5.minutes)

                // We are not as important
                yield()

                // "Live" client must have activity within 5 minutes
                val newClients =
                    seenClients.updateAndGet { set ->
                      set.filter {
                            val old = it.mostRecentlySeen >= beforeWait
                            if (old) {
                              Timber.d("Client is too old: $it")
                            }
                            return@filter old
                          }
                          .toSet()
                    }
                blockedClients.update { set ->
                  set.filter { bc ->
                        // If this blocked client is still found in the "new client" list, keep it,
                        // otherwise filter it out
                        val stillAlive = newClients.firstOrNull { nc -> isMatchingClient(bc, nc) }
                        return@filter stillAlive != null
                      }
                      .toSet()
                }
              }
            }
  }

  override fun block(client: TetherClient) {
    blockedClients.update { set ->
      val existing = set.firstOrNull { isMatchingClient(it, client) }

      return@update set.run {
        if (existing == null) {
          this + client
        } else {
          this
        }
      }
    }
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
    seenClients.update { set ->
      val existing = set.firstOrNull { isMatchingClient(it, client) }

      if (existing == null) {
        return@update (set + client).also { onNewClientSeen(client) }
      } else {
        return@update set.map { c ->
              if (c == existing) {
                return@map markLastSeenNow(c)
              } else {
                return@map c
              }
            }
            .toSet()
      }
    }
  }

  override fun clear() {
    seenClients.value = emptySet()
    blockedClients.value = emptySet()

    watchJob?.cancel()
    scope.cancelChildren()
  }

  override fun listenForClients(): Flow<Set<TetherClient>> {
    return seenClients
  }

  override fun listenForBlocked(): Flow<Set<TetherClient>> {
    return blockedClients
  }
}
