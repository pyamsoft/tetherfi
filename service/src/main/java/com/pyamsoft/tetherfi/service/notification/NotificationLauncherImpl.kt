package com.pyamsoft.tetherfi.service.notification

import android.app.Service
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.SeenClients
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationLauncherImpl
@Inject
internal constructor(
    @ServiceInternalApi private val notifier: Notifier,
    private val networkStatus: WiDiNetworkStatus,
    private val seenClients: SeenClients,
    private val blockedClients: BlockedClients,
) : NotificationLauncher {

  private val scope = MainScope()

  private var notificationJob: Job? = null

  private fun onStatusUpdated(
      service: Service,
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
        .startForeground(
            service = service,
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Updated foreground notification: $it: $data") }
  }

  private fun watchNotification(service: Service) {
    // These are updated in line
    var clientCount = 0
    var blockCount = 0
    var runningStatus: RunningStatus = RunningStatus.NotRunning

    // Private scoped is kind of a hack but here we are
    // I just don't want to declare globals outside of this scope as they are unreliable
    fun updateNotification() {
      onStatusUpdated(
          service = service,
          status = runningStatus,
          clientCount = clientCount,
          blockCount = blockCount,
      )
    }

    notificationJob?.cancel()
    notificationJob =
        // Supervisor job will cancel all children
        scope.launch(context = Dispatchers.Default) {

          // Listen for notification updates
          launch(context = Dispatchers.Default) {
            networkStatus.onStatusChanged {
              runningStatus = it
              updateNotification()
            }
          }

          // Listen for client updates
          launch(context = Dispatchers.Default) {
            seenClients.listenForClients().collectLatest {
              clientCount = it.size
              updateNotification()
            }
          }

          // Listen for block updates
          launch(context = Dispatchers.Default) {
            blockedClients.listenForBlocked().collectLatest {
              blockCount = it.size
              updateNotification()
            }
          }
        }
  }

  override fun start(service: Service) {
    val data = DEFAULT_DATA

    // Initialize with blank data first
    notifier
        .startForeground(
            service = service,
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Started foreground notification: $it: $data") }

    // Then immediately open a channel to update
    watchNotification(service)
  }

  override fun stop(service: Service) {
    notifier.stopForeground(
        service = service,
        id = NOTIFICATION_ID,
    )

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
