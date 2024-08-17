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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.contains
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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
) :
    BlockedClientTracker,
    BlockedClients,
    AllowedClients,
    ClientEraser,
    StartedClients,
    ClientResolver,
    ClientEditor {

  private val blockedClients = MutableStateFlow<Collection<TetherClient>>(emptySet())
  private val allowedClients = MutableStateFlow<Collection<TetherClient>>(emptySet())

  private val jobs = MutableStateFlow<Collection<Job>>(emptySet())

  @CheckResult
  private fun markLastSeenNow(client: TetherClient): TetherClient =
      when (client) {
        is IpAddressClient -> client.copy(mostRecentlySeen = LocalDateTime.now(clock))
        is HostNameClient -> client.copy(mostRecentlySeen = LocalDateTime.now(clock))
      }

  @CheckResult
  private fun editNickName(client: TetherClient, nickName: String): TetherClient =
      when (client) {
        is IpAddressClient -> client.copy(nickName = nickName)
        is HostNameClient -> client.copy(nickName = nickName)
      }

  @CheckResult
  private fun editTransferLimit(client: TetherClient, limit: TransferAmount?): TetherClient =
      when (client) {
        is IpAddressClient -> client.copy(transferLimit = limit)
        is HostNameClient -> client.copy(transferLimit = limit)
      }

  @CheckResult
  private fun updateTransferReport(client: TetherClient, report: ByteTransferReport): TetherClient =
      when (client) {
        is IpAddressClient -> client.copy(totalBytes = client.mergeReport(report))
        is HostNameClient -> client.copy(totalBytes = client.mergeReport(report))
      }

  private fun onNewClientSeen(client: TetherClient) {
    Timber.d { "First time seeing client: $client" }
    inAppRatingPreferences.markDeviceConnected()
  }

  @Suppress("UnusedReceiverParameter")
  private fun CoroutineScope.onClientsUpdated(
      @Suppress("UNUSED_PARAMETER") clients: Collection<TetherClient>,
  ) {
    // TODO(Peter): Should we restart the watchForNoClients timer here?
    //              This function could be called a bunch and it would cause tons of scopes to be
    //              created and cancelled, which could kill performance.
  }

  private fun purgeOldClients(cutoffTime: LocalDateTime) {
    Timber.d { "Attempt to purge old clients before $cutoffTime" }

    // "Live" client must have activity within 2 minutes
    val newClients =
        allowedClients.updateAndGet { list ->
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
            val stillAlive = newClients.firstOrNull { bc.matches(it) }
            return@filter stillAlive != null
          }
          .toSet()
    }

    // Don't call shutdownWithNoClients here, we want to call it only on its own schedule
  }

  private suspend fun shutdownWithNoClients() {
    if (isShutdownWithNoClientsEnabled()) {
      if (allowedClients.value.isEmpty()) {
        Timber.d { "No clients are connected. Shutdown Proxy!" }
        shutdownBus.emit(ServerShutdownEvent)
      }
    }
  }

  private fun CoroutineScope.watchForOldClients() {
    val scope = this

    jobs.update { j ->
      j +
          scope.launch(context = Dispatchers.Default) {
            Timber.d { "Watch client count and purge old clients" }
            onTimerElapsed(OLD_CLIENT_TIMER_PERIOD) { purgeOldClients(it) }
          }
    }
  }

  @CheckResult
  private suspend fun isShutdownWithNoClientsEnabled(): Boolean {
    return serverPreferences.listenForShutdownWithNoClients().first()
  }

  private suspend fun CoroutineScope.watchForNoClients() {
    val scope = this

    // Start new if needed
    if (isShutdownWithNoClientsEnabled()) {
      // Remember when we started watching
      //
      // We do this weird check because in development I noticed when starting the server
      // once a random ShutdownWithNoClients was published during startup, which caused
      // the app to break since Broadcast started but Proxy never started
      val startedAt = LocalDateTime.now(clock)

      jobs.update { j ->
        j +
            scope.launch(context = Dispatchers.Default) {
              Timber.d { "Watch client count and shutdown if none" }
              onTimerElapsed(NO_CLIENTS_TIMER_PERIOD) { cutoff ->
                if (startedAt >= cutoff) {
                  Timber.w { "Shutdown check received but client started AFTER cutoff - invalid" }
                } else {
                  shutdownWithNoClients()
                }
              }
            }
      }
    }
  }

  @CheckResult
  private fun isBlockedClient(client: TetherClient): Boolean {
    return blockedClients.value.contains { it.matches(client) }
  }

  private suspend inline fun onTimerElapsed(period: Duration, block: (LocalDateTime) -> Unit) {
    try {
      while (true) {
        delay(period)
        val cutoffTime = LocalDateTime.now(clock).minusMinutes(period.inWholeMinutes)

        Timber.d { "Timer elapsed. Cutoff: $cutoffTime" }
        block(cutoffTime)
      }
    } finally {
      Timber.d { "Timer completed." }
    }
  }

  @CheckResult
  private fun checkBlocked(client: TetherClient): Boolean {
    // Check Blocklist
    if (isBlockedClient(client)) {
      Timber.w { "Hard blocked client: $client" }
      return true
    }

    // Check we are not over the transfer limit
    if (client.isOverTransferLimit()) {
      Timber.w { "Transfer limited client: $client" }
      return true
    }

    return false
  }

  @CheckResult
  private fun resolve(hostNameOrIp: String): TetherClient? {
    val allowed = allowedClients.value

    // Find a client
    return allowed.firstOrNull { it.matches(hostNameOrIp) }
  }

  private fun CoroutineScope.handleClientUpdate(
      client: TetherClient,
      onClientUpdated: (TetherClient) -> TetherClient,
  ) {
    val clients =
        allowedClients.updateAndGet { list ->
          return@updateAndGet list.map { c ->
            if (c.matches(client)) {
              return@map onClientUpdated(c)
            } else {
              return@map c
            }
          }
        }

    onClientsUpdated(clients)
  }

  override suspend fun started() =
      withContext(context = Dispatchers.Default) {
        watchForOldClients()
        watchForNoClients()
      }

  override fun block(client: TetherClient) {
    blockedClients.update { set ->
      val existing = set.firstOrNull { it.matches(client) }

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
      val existing = clients.firstOrNull { it.matches(client) }

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
    return checkBlocked(client)
  }

  override suspend fun seen(client: TetherClient) =
      withContext(context = Dispatchers.Default) {
        handleClientUpdate(client) { markLastSeenNow(it) }
      }

  override suspend fun updateNickName(client: TetherClient, nickName: String) =
      withContext(context = Dispatchers.Default) {
        handleClientUpdate(client) { editNickName(it, nickName) }
      }

  override suspend fun updateTransferLimit(client: TetherClient, limit: TransferAmount?) =
      withContext(context = Dispatchers.Default) {
        handleClientUpdate(client) { editTransferLimit(it, limit) }
      }

  override suspend fun reportTransfer(client: TetherClient, report: ByteTransferReport) =
      withContext(context = Dispatchers.Default) {
        handleClientUpdate(client) { updateTransferReport(it, report) }
      }

  override fun clear() {
    Timber.d { "Clear client tracker" }

    allowedClients.value = emptySet()
    blockedClients.value = emptySet()
    jobs.value = emptySet()
  }

  override fun listenForClients(): Flow<Collection<TetherClient>> {
    return allowedClients
  }

  override fun listenForBlocked(): Flow<Collection<TetherClient>> {
    return blockedClients
  }

  override fun ensure(hostNameOrIp: String): TetherClient {
    val maybeClient = resolve(hostNameOrIp)
    return maybeClient
        ?: allowedClients
            .updateAndGet {
              val client =
                  TetherClient.create(
                      hostNameOrIp = hostNameOrIp,
                      clock = clock,
                  )
              return@updateAndGet (it + client).also { onNewClientSeen(client) }
            }
            .firstOrNull { it.matches(hostNameOrIp) }
            .requireNotNull { "Unable to ensure TetherClient exists: $hostNameOrIp" }
  }

  companion object {
    /** Purge old or inactive clients after 5 minutes of no activity */
    private val OLD_CLIENT_TIMER_PERIOD = 5.minutes

    /** Shut down the hotspot after 10 minutes of absolutely zero client activity */
    private val NO_CLIENTS_TIMER_PERIOD = 10.minutes
  }
}
