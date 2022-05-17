package com.pyamsoft.widefi.activity

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.ui.logging.LogStorage
import dagger.Binds
import dagger.Module

@Module
abstract class ActivityModule {

  @Binds
  @CheckResult
  internal abstract fun bindLogStorage(impl: ActivityLogStorage): LogStorage<ConnectionEvent>
}
