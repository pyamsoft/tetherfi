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

package com.pyamsoft.tetherfi.service.notification

import android.app.Service
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
internal class NotificationLauncherImpl
@Inject
internal constructor(
    @ServiceInternalApi private val notifier: Notifier,
    private val enforcer: ThreadEnforcer,
    private val networkStatus: BroadcastNetworkStatus,
    private val allowedClients: AllowedClients,
    private val blockedClients: BlockedClients,
) : NotificationLauncher {

  private val showing = MutableStateFlow(false)

  private val clientCount = MutableStateFlow(0)
  private val blockCount = MutableStateFlow(0)
  private val runningStatus = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  private suspend fun onStatusUpdated(
      status: RunningStatus,
      clientCount: Int,
      blockCount: Int,
  ) {
    if (!showing.value) {
      Timber.w { "Do not update notification, no longer showing!" }
      return
    }

    withContext(context = Dispatchers.Main) {
      val data =
          ServerNotificationData(
              status = status,
              clientCount = clientCount,
              blockCount = blockCount,
          )

      notifier
          .show(
              id = LONG_RUNNING_ID,
              channelInfo = LONG_RUNNING_CHANNEL_INFO,
              notification = data,
          )
          .also { Timber.d { "Updated foreground notification: $it: $data" } }
    }
  }

  @CheckResult
  private fun <T> MutableStateFlow<T>.compareCurrent(update: T): Boolean {
    val current = this.value
    if (current != update) {
      this.value = update
      return true
    }

    return false
  }

  private suspend fun updateNotification() {
    onStatusUpdated(
        status = runningStatus.value,
        clientCount = clientCount.value,
        blockCount = blockCount.value,
    )
  }

  private fun CoroutineScope.watchNotification() {
    val scope = this

    // Supervisor job will cancel all children
    scope.launch {
      enforcer.assertOffMainThread()

      // Listen for notification updates
      networkStatus.onStatusChanged().also { f ->
        launch {
          enforcer.assertOffMainThread()

          f.collect { s ->
            if (runningStatus.compareCurrent(s)) {
              updateNotification()
            }
          }
        }
      }

      // Listen for client updates
      allowedClients
          .listenForClients()
          .map { it.size }
          .also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect { c ->
                if (clientCount.compareCurrent(c)) {
                  updateNotification()
                }
              }
            }
          }

      // Listen for block updates
      blockedClients
          .listenForBlocked()
          .map { it.size }
          .also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect { b ->
                if (blockCount.compareCurrent(b)) {
                  updateNotification()
                }
              }
            }
          }
    }
  }

  private fun reset() {
    clientCount.value = 0
    blockCount.value = 0
    runningStatus.value = RunningStatus.NotRunning
  }

  private fun stop(service: Service) {
    enforcer.assertOnMainThread()

    Timber.d { "Stop foreground notification" }
    notifier.stopForeground(service, LONG_RUNNING_ID)

    reset()
  }

  private fun start(service: Service) {
    // Hold this here until the coroutine is cancelled
    reset()

    // Initialize with blank data first
    val data = LONG_RUNNING_DEFAULT_DATA
    notifier
        .startForeground(
            service = service,
            id = LONG_RUNNING_ID,
            channelInfo = LONG_RUNNING_CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d { "Started foreground notification: $it: $data" } }
  }

  override suspend fun hideError() =
      withContext(context = Dispatchers.Default) {
        notifier.cancel(ERROR_ID)
        return@withContext
      }

  override suspend fun showError(throwable: Throwable) =
      withContext(context = Dispatchers.Default) {
        // Hide any old ones first
        hideError()

        // Show a new one
        notifier
            .show(
                id = ERROR_ID,
                channelInfo = ERROR_CHANNEL_INFO,
                notification = ErrorNotificationData(throwable),
            )
            .also { Timber.d { "Show error notification: $it: $throwable" } }
        return@withContext
      }

  override suspend fun update() =
      withContext(context = Dispatchers.Default) {
        if (!showing.value) {
          Timber.w { "Cannot update notification since not showing" }
          return@withContext
        }

        updateNotification()
      }

  override fun startForeground(service: Service): NotificationLauncher.Watcher {
    if (showing.compareAndSet(expect = false, update = true)) {
      start(service)

      return NotificationLauncher.Watcher { scope: CoroutineScope ->
        scope.launch(context = Dispatchers.Default) {
          try {
            Timber.d { "Launch notification watcher" }
            // Then immediately open a channel to update
            watchNotification()

            // And suspend until we are done
            Timber.d { "Await notification cancellation..." }
            awaitCancellation()
          } finally {
            withContext(context = NonCancellable) {
              Timber.d { "Notification scope is done, cancel notification!" }
              if (showing.compareAndSet(expect = true, update = false)) {
                withContext(context = Dispatchers.Main) { stop(service) }
              }
            }
          }
        }
      }
    } else {
      Timber.w { "Notification is already started, return EMPTY_WATCHER" }
      return EMPTY_WATCHER
    }
  }

  companion object {

    private val LONG_RUNNING_ID = 42069.toNotifyId()
    private val LONG_RUNNING_DEFAULT_DATA =
        ServerNotificationData(
            status = RunningStatus.NotRunning,
            clientCount = 0,
            blockCount = 0,
        )
    private val LONG_RUNNING_CHANNEL_INFO =
        NotifyChannelInfo(
            id = "channel_tetherfi_service_1",
            title = "TetherFi Proxy",
            description = "TetherFi Proxy Service",
        )

    private val ERROR_ID = 133710.toNotifyId()
    private val ERROR_CHANNEL_INFO =
        NotifyChannelInfo(
            id = "channel_tetherfi_error_1",
            title = "TetherFi Errors",
            description = "TetherFi Errors",
        )

    private val EMPTY_WATCHER = NotificationLauncher.Watcher {}
  }
}
