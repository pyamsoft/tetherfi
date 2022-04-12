package com.pyamsoft.widefi.service

import android.app.Service

interface NotificationLauncher {

  fun start(service: Service)

  fun stop(service: Service)
}
