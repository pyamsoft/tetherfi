package com.pyamsoft.tetherfi.service

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyDispatcher
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.service.lock.Locker
import com.pyamsoft.tetherfi.service.lock.LockerImpl
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import com.pyamsoft.tetherfi.service.notification.NotificationLauncherImpl
import com.pyamsoft.tetherfi.service.notification.ServiceDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
abstract class ServiceModule {

  @Binds
  @IntoSet
  @ServiceInternalApi
  internal abstract fun bindServiceDispatcher(impl: ServiceDispatcher): NotifyDispatcher<*>

  @Binds internal abstract fun bindLauncher(impl: NotificationLauncherImpl): NotificationLauncher

  @Binds @ServiceInternalApi internal abstract fun bindLocker(impl: LockerImpl): Locker

  @Binds
  @CheckResult
  internal abstract fun bindNotificationRefreshConsumer(
      impl: EventBus<NotificationRefreshEvent>
  ): EventConsumer<NotificationRefreshEvent>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideNotificationRefreshEventBus(): EventBus<NotificationRefreshEvent> {
      return EventBus.create()
    }

    @Provides
    @Singleton
    @JvmStatic
    @CheckResult
    @ServiceInternalApi
    internal fun provideNotifier(
        // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
        @ServiceInternalApi dispatchers: MutableSet<NotifyDispatcher<*>>,
        context: Context
    ): Notifier {
      return Notifier.createDefault(context, dispatchers)
    }
  }
}
