package com.pyamsoft.widefi.error

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.ui.logging.LogStorage
import dagger.Binds
import dagger.Module

@Module
abstract class ErrorModule {

  @Binds
  @CheckResult
  internal abstract fun bindLogStorage(impl: ErrorLogStorage): LogStorage<ErrorEvent>
}
