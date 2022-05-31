package com.pyamsoft.tetherfi.service.notification

import android.app.Service

interface NotificationLauncher {

  fun start(service: Service)

  fun stop(service: Service)
}
