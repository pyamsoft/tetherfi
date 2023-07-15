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

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyDispatcher
import com.pyamsoft.pydroid.notify.NotifyId
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.R
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class ServiceDispatcher
@Inject
internal constructor(
    private val context: Context,
    @Named("main_activity") private val activityClass: Class<out Activity>,
    @StringRes @Named("app_name") private val appNameRes: Int,
) : NotifyDispatcher<ServerNotificationData> {

  private val channelCreator by lazy {
    context.applicationContext.getSystemService<NotificationManager>().requireNotNull()
  }

  @CheckResult
  private fun getActivityPendingIntent(): PendingIntent {
    val appContext = context.applicationContext
    val activityIntent =
        Intent(appContext, activityClass).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    return PendingIntent.getActivity(
        appContext,
        REQUEST_CODE_ACTIVITY,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun guaranteeNotificationChannelExists(channelInfo: NotifyChannelInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationGroup =
          NotificationChannelGroup("${channelInfo.id} Group", "${channelInfo.title} Group")
      val notificationChannel =
          NotificationChannel(channelInfo.id, channelInfo.title, NotificationManager.IMPORTANCE_LOW)
              .apply {
                group = notificationGroup.id
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = channelInfo.description
                enableLights(false)
                enableVibration(true)
              }

      channelCreator.apply {
        // Delete the group if it already exists with a bad group ID
        // Group ID and channel ID cannot match
        if (notificationChannelGroups.firstOrNull { it.id == channelInfo.id } != null) {
          deleteNotificationChannelGroup(channelInfo.id)
        }
        createNotificationChannelGroup(notificationGroup)
        createNotificationChannel(notificationChannel)
      }
    }
  }

  @CheckResult
  private fun createNotificationBuilder(
      channelInfo: NotifyChannelInfo
  ): NotificationCompat.Builder {
    return NotificationCompat.Builder(context.applicationContext, channelInfo.id)
        .setSmallIcon(R.drawable.ic_wifi_tethering_24)
        .setShowWhen(false)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(getActivityPendingIntent())
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
  }

  @CheckResult
  private fun resolveContentText(
      appName: String,
      notification: ServerNotificationData,
  ): String {
    return when (notification.status) {
      is RunningStatus.Error -> "Hotspot Error. Please open $appName and restart the Hotspot."
      is RunningStatus.NotRunning -> "Hotspot preparing..."
      is RunningStatus.Running ->
          "Hotspot Ready. ${notification.clientCount} Clients. ${notification.blockCount} Blocked."
      is RunningStatus.Starting -> "Hotspot starting..."
      is RunningStatus.Stopping -> "Hotspot stopping..."
    }
  }

  override fun build(
      id: NotifyId,
      channelInfo: NotifyChannelInfo,
      notification: ServerNotificationData
  ): Notification {
    val appName = context.getString(appNameRes)
    guaranteeNotificationChannelExists(channelInfo)
    return createNotificationBuilder(channelInfo)
        .setContentTitle(appName)
        .setContentText(resolveContentText(appName, notification))
        .build()
  }

  override fun canShow(notification: NotifyData): Boolean {
    return notification is ServerNotificationData
  }

  companion object {

    private const val REQUEST_CODE_ACTIVITY = 1337420
  }
}
