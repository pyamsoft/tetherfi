package com.pyamsoft.tetherfi.service

import android.app.Service

interface NotificationLauncher {

  fun start(service: Service)

  fun stop(service: Service)
}
