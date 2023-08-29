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
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
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
import kotlinx.coroutines.flow.first
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
    private val inAppRatingPreferences: InAppRatingPreferences,
    private val clock: Clock,
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val serverPreferences: ServerPreferences,
    enforcer: ThreadEnforcer,
) : BlockedClientTracker, BlockedClients, SeenClients, ClientEraser, StartedClients {

  private val blockedClients = MutableStateFlow<Collection<TetherClient>>(mutableSetOf())
  private val seenClients = MutableStateFlow<List<TetherClient>>(mutableListOf())

  private val oldClientCheck =
      TimerTrigger(
          clock = clock,
          enforcer = enforcer,
          timerPeriod = OLD_CLIENT_TIMER_PERIOD,
      )

  private val noClientCheck =
      TimerTrigger(
          clock = clock,
          enforcer = enforcer,
          timerPeriod = NO_CLIENTS_TIMER_PERIOD,
      )

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

    watchForOldClients()
  }

  @Suppress("UnusedReceiverParameter")
  private fun CoroutineScope.onClientsUpdated(
      @Suppress("UNUSED_PARAMETER") clients: List<TetherClient>,
  ) {
    // TODO(Peter): Should we restart the watchForNoClients timer here?
    //              This function could be called a bunch and it would cause tons of scopes to be
    //              created and cancelled, which could kill performance.
  }

  private suspend fun purgeOldClients(cutoffTime: LocalDateTime) {
    Timber.d { "Attempt to purge old clients before $cutoffTime" }

    // "Live" client must have activity within 2 minutes
    val newClients =
        seenClients.updateAndGet { list ->
          list.filter {
            val newEnough = it.mostRecentlySeen >= cutoffTime
            if (!newEnough) {
              Timber.d { "Client is too old: $it. Last seen ${it.mostRecentlySeen}" }
            }
            return@filter newEnough
          }
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

    shutdownWithNoClients()
  }

  private suspend fun shutdownWithNoClients() {
    if (seenClients.value.isEmpty()) {
      Timber.d { "No clients are connected. Shutdown Proxy!" }
      shutdownBus.emit(ServerShutdownEvent)
    }
  }

  private fun CoroutineScope.watchForOldClients() {
    oldClientCheck.start(scope = this) { purgeOldClients(it) }
  }

  private suspend fun CoroutineScope.watchForNoClients() {
    if (serverPreferences.listenForShutdownWithNoClients().first()) {
      noClientCheck.start(
          scope = this,
          initialDelay = NO_CLIENTS_TIMER_PERIOD,
      ) {
        shutdownWithNoClients()
      }
    } else {
      noClientCheck.cancel()
    }
  }

  override suspend fun started() =
      withContext(context = Dispatchers.Default) { watchForNoClients() }

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
        val clients =
            seenClients.updateAndGet { list ->
              val existing = list.firstOrNull { isMatchingClient(it, client) }

              if (existing == null) {
                return@updateAndGet (list + client).also { onNewClientSeen(client) }
              } else {
                return@updateAndGet list.map { c ->
                  if (c == existing) {
                    return@map markLastSeenNow(c)
                  } else {
                    return@map c
                  }
                }
              }
            }

        onClientsUpdated(clients)
      }

  override fun clear() {
    seenClients.value = emptyList()
    blockedClients.value = emptySet()

    oldClientCheck.cancel()
    noClientCheck.cancel()
  }

  override fun listenForClients(): Flow<List<TetherClient>> {
    return seenClients
  }

  override fun listenForBlocked(): Flow<Collection<TetherClient>> {
    return blockedClients
  }

  private class TimerTrigger(
      private val clock: Clock,
      private val enforcer: ThreadEnforcer,
      private val timerPeriod: Duration,
  ) {

    private var job: Job? = null

    @CheckResult
    private fun timerFlow(
        periodInMillis: Duration,
        initialDelay: Duration,
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

    fun start(
        scope: CoroutineScope,
        initialDelay: Duration = Duration.ZERO,
        onTrigger: suspend (LocalDateTime) -> Unit,
    ) {
      job =
          job
              ?: timerFlow(timerPeriod, initialDelay).let { f ->
                scope.launch(context = Dispatchers.IO) {
                  f.collect {
                    // Cutoff time is X minutes ago
                    val cutoffTime =
                        LocalDateTime.now(clock).minusMinutes(timerPeriod.inWholeMinutes)
                    onTrigger(cutoffTime)
                  }
                }
              }
    }

    fun cancel() {
      Timber.d { "Stopping timer flow $timerPeriod" }
      job?.cancel()
      job = null
    }
  }

  companion object {
    private val OLD_CLIENT_TIMER_PERIOD = 2.minutes
    private val NO_CLIENTS_TIMER_PERIOD = 10.minutes
  }
}
