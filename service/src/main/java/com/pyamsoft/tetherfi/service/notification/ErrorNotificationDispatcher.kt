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

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.tetherfi.service.R
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class ErrorNotificationDispatcher
@Inject
internal constructor(
    context: Context,
    @Named("main_activity") mainActivityClass: Class<out Activity>,
    @StringRes @Named("app_name") appNameRes: Int,
) : BaseDispatcher<ErrorNotificationData>(
    context = context,
    mainActivityClass = mainActivityClass,
    appNameRes = appNameRes,
) {

    override fun onCreateNotificationBuilder(
        appName: String,
        notification: ErrorNotificationData,
        builder: NotificationCompat.Builder
    ): NotificationCompat.Builder = builder
        .setSmallIcon(R.drawable.ic_wifi_tethering_24)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setContentTitle("$appName Error")
        .setContentInfo("$appName Error")
        .setSubText("$appName Error")
        .setContentText(
            notification.throwable.message.orEmpty()
                .ifBlank { "An unexpected error occurred. Please restart the $appName Hotspot." })

    override fun canShow(notification: NotifyData): Boolean {
        return notification is ErrorNotificationData
    }

}
