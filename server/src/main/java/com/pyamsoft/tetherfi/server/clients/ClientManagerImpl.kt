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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
internal class ClientManagerImpl
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
    private val inAppRatingPreferences: InAppRatingPreferences,
    private val clock: Clock,
) : BlockedClientTracker, BlockedClients, SeenClients, ClientEraser {

  private val blockedClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())
  private val seenClients = MutableStateFlow<Set<TetherClient>>(mutableSetOf())

  private var timerJob: Job? = null

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

  private fun CoroutineScope.onNewClientSeen(client: TetherClient) {
    Timber.d { "First time seeing client: $client" }
    inAppRatingPreferences.markDeviceConnected()

    startWatchingForOldClients()
  }

  private fun purgeOldClients(cutoffTime: LocalDateTime) {
    Timber.d { "Attempt to purge old clients before $cutoffTime" }

    // "Live" client must have activity within 5 minutes
    val newClients =
        seenClients.updateAndGet { set ->
          set.filter {
                val newEnough = it.mostRecentlySeen >= cutoffTime
                if (!newEnough) {
                  Timber.d { "Client is too old: $it. Last seen ${it.mostRecentlySeen}" }
                }
                return@filter newEnough
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

  @CheckResult
  private fun timerFlow(
      periodInMillis: Duration,
      initialDelay: Duration = Duration.ZERO,
  ): Flow<Unit> =
      flow {
            enforcer.assertOffMainThread()
            val ctx = currentCoroutineContext()

            delay(initialDelay)

            while (ctx.isActive) {
              emit(Unit)
              delay(periodInMillis)
            }
          }
          .flowOn(context = Dispatchers.IO)

  private fun CoroutineScope.startWatchingForOldClients() {
    val scope = this

    timerJob =
        timerJob
            ?: timerFlow(PERIOD_IN_MINUTES.minutes).let { f ->
              Timber.d {
                "Start a timer flow to check old clients every $PERIOD_IN_MINUTES minutes"
              }
              scope.launch(context = Dispatchers.IO) {
                f.collect {
                  // Cutoff time is X minutes ago
                  val cutoffTime = LocalDateTime.now(clock).minusMinutes(PERIOD_IN_MINUTES)
                  purgeOldClients(cutoffTime)
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

  override suspend fun seen(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
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
    timerJob?.cancel()

    seenClients.value = emptySet()
    blockedClients.value = emptySet()

    timerJob = null
  }

  override fun listenForClients(): Flow<Set<TetherClient>> {
    return seenClients
  }

  override fun listenForBlocked(): Flow<Set<TetherClient>> {
    return blockedClients
  }

  companion object {

    private const val PERIOD_IN_MINUTES = 5L
  }
}
