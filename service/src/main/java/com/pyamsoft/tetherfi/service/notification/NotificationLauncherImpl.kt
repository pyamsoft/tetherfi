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

package com.pyamsoft.tetherfi.service.notification

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.SeenClients
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationLauncherImpl
@Inject
internal constructor(
    @ServiceInternalApi private val notifier: Notifier,
    private val enforcer: ThreadEnforcer,
    private val networkStatus: WiDiNetworkStatus,
    private val seenClients: SeenClients,
    private val blockedClients: BlockedClients,
) : NotificationLauncher {

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
    )
  }

  private var notificationJob: Job? = null

  private fun onStatusUpdated(
      status: RunningStatus,
      clientCount: Int,
      blockCount: Int,
  ) {
    val data =
        ServerNotificationData(
            status = status,
            clientCount = clientCount,
            blockCount = blockCount,
        )

    notifier
        .show(
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Updated foreground notification: $it: $data") }
  }

  private fun watchNotification() {
    // These are updated in line
    var clientCount = 0
    var blockCount = 0
    var runningStatus: RunningStatus = RunningStatus.NotRunning

    // Private scoped is kind of a hack but here we are
    // I just don't want to declare globals outside of this scope as they are unreliable
    fun updateNotification() {
      onStatusUpdated(
          status = runningStatus,
          clientCount = clientCount,
          blockCount = blockCount,
      )
    }

    notificationJob?.cancel()
    notificationJob =
        // Supervisor job will cancel all children
        scope.launch {
          enforcer.assertOffMainThread()

          // Listen for notification updates
          networkStatus.onStatusChanged().also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect { s ->
                if (runningStatus != s) {
                  runningStatus = s
                  updateNotification()
                }
              }
            }
          }

          // Listen for client updates
          seenClients
              .listenForClients()
              .map { it.size }
              .also { f ->
                launch {
                  enforcer.assertOffMainThread()

                  f.collect { c ->
                    if (clientCount != c) {
                      clientCount = c
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
                    if (blockCount != b) {
                      blockCount = b
                      updateNotification()
                    }
                  }
                }
              }
        }
  }

  override fun start() {
    val data = DEFAULT_DATA

    // Initialize with blank data first
    notifier
        .show(
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Started foreground notification: $it: $data") }

    // Then immediately open a channel to update
    watchNotification()
  }

  override fun stop() {
    notifier.cancel(NOTIFICATION_ID)
    notificationJob?.cancel()
    notificationJob = null
  }

  companion object {

    private val NOTIFICATION_ID = 42069.toNotifyId()

    private val DEFAULT_DATA =
        ServerNotificationData(
            status = RunningStatus.NotRunning,
            clientCount = 0,
            blockCount = 0,
        )

    private val CHANNEL_INFO =
        NotifyChannelInfo(
            id = "channel_tetherfi_service_1",
            title = "TetherFi Proxy",
            description = "TetherFi Proxy Service",
        )
  }
}
