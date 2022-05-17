package com.pyamsoft.widefi.activity

import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.ui.logging.ApplicationLogStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ActivityLogStorage @Inject internal constructor() :
    ApplicationLogStorage<ConnectionEvent>()
