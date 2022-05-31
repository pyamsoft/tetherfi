package com.pyamsoft.tetherfi.service.notification

import android.app.Service
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
internal class NotificationLauncherImpl
@Inject
internal constructor(
    @ServiceInternalApi private val notifier: Notifier,
) : NotificationLauncher {
  override fun start(service: Service) {
    notifier
        .startForeground(
            service = service,
            id = NOTIFICATION_ID,
            channelInfo = CHANNEL_INFO,
            notification = NotificationData,
        )
        .also { Timber.d("Started foreground notification: $it") }
  }

  override fun stop(service: Service) {
    notifier.stopForeground(
        service = service,
        id = NOTIFICATION_ID,
    )
  }

  companion object {

    private val NOTIFICATION_ID = 42069.toNotifyId()

    private val CHANNEL_INFO =
        NotifyChannelInfo(
            id = "channel_tetherfi_service_1",
            title = "TetherFi Proxy",
            description = "TetherFi Proxy Service",
        )
  }
}
