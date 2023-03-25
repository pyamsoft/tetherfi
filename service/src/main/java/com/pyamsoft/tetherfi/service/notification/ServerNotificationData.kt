package com.pyamsoft.tetherfi.service.notification

import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.tetherfi.server.status.RunningStatus

internal data class ServerNotificationData
internal constructor(
    val status: RunningStatus,
    val clientCount: Int,
    val blockCount: Int,
) : NotifyData
