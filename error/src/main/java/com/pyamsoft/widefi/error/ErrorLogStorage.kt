package com.pyamsoft.widefi.error

import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.ui.logging.ApplicationLogStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ErrorLogStorage @Inject internal constructor() : ApplicationLogStorage<ErrorEvent>()
