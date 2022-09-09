package com.pyamsoft.tetherfi.service.notification

import android.app.Service
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
internal class NotificationLauncherImpl
@Inject
internal constructor(
    @ServiceInternalApi private val notifier: Notifier,
    private val networkStatus: WiDiNetworkStatus,
) : NotificationLauncher {

  private val scope = MainScope()

  private var statusJob: Job? = null

  private fun onStatusUpdated(
      service: Service,
      status: RunningStatus,
  ) {
    val data = ServerNotificationData(status = status)

    notifier
        .startForeground(
            service = service,
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Updated foreground notification: $it: $data") }
  }

  override fun start(service: Service) {
    val data = DEFAULT_DATA

    notifier
        .startForeground(
            service = service,
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = data,
        )
        .also { Timber.d("Started foreground notification: $it: $data") }

    statusJob?.cancel()
    statusJob =
        scope.launch(context = Dispatchers.Main) {
          networkStatus.onStatusChanged { status -> onStatusUpdated(service, status) }
        }
  }

  override fun stop(service: Service) {

    notifier.stopForeground(
        service = service,
        id = NOTIFICATION_ID,
    )

    statusJob?.cancel()
    statusJob = null
  }

  companion object {

    private val NOTIFICATION_ID = 42069.toNotifyId()

    private val DEFAULT_DATA = ServerNotificationData(status = RunningStatus.NotRunning)

    private val CHANNEL_INFO =
        NotifyChannelInfo(
            id = "channel_tetherfi_service_1",
            title = "TetherFi Proxy",
            description = "TetherFi Proxy Service",
        )
  }
}
