/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.service

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.bus.internal.DefaultEventBus
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyDispatcher
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.service.lock.Locker
import com.pyamsoft.tetherfi.service.lock.LockerImpl
import com.pyamsoft.tetherfi.service.lock.WakeLocker
import com.pyamsoft.tetherfi.service.lock.WiFiLocker
import com.pyamsoft.tetherfi.service.notification.ErrorNotificationDispatcher
import com.pyamsoft.tetherfi.service.notification.LongRunningServiceDispatcher
import com.pyamsoft.tetherfi.service.notification.NotificationLauncher
import com.pyamsoft.tetherfi.service.notification.NotificationLauncherImpl
import com.pyamsoft.tetherfi.service.prereq.AndroidHotspotRequirements
import com.pyamsoft.tetherfi.service.prereq.HotspotRequirements
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
abstract class ServiceAppModule {

  @Binds internal abstract fun bindLauncher(impl: NotificationLauncherImpl): NotificationLauncher

  @Binds internal abstract fun bindPrereqs(impl: AndroidHotspotRequirements): HotspotRequirements

  // TODO(Peter): Move into some shared module and scope
  @Binds
  @IntoSet
  @ServiceInternalApi
  internal abstract fun bindLongRunningServiceDispatcher(
      impl: LongRunningServiceDispatcher
  ): NotifyDispatcher<*>

  // TODO(Peter): Move into some shared module and scope
  @Binds
  @IntoSet
  @ServiceInternalApi
  internal abstract fun bindErrorDispatcher(impl: ErrorNotificationDispatcher): NotifyDispatcher<*>

  @Binds
  @CheckResult
  internal abstract fun bindNotificationRefreshConsumer(
      impl: EventBus<NotificationRefreshEvent>
  ): EventConsumer<NotificationRefreshEvent>

  // Lockers
  @Binds @IntoSet @ServiceInternalApi internal abstract fun bindWakeLocker(impl: WakeLocker): Locker

  @Binds @IntoSet @ServiceInternalApi internal abstract fun bindWiFiLocker(impl: WiFiLocker): Locker

  @Binds @ServiceInternalApi internal abstract fun bindLocker(impl: LockerImpl): Locker

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideNotificationRefreshEventBus(): EventBus<NotificationRefreshEvent> {
      return DefaultEventBus()
    }

    // TODO(Peter): Move into some shared module and scope
    @Provides
    @Singleton
    @JvmStatic
    @CheckResult
    internal fun provideNotifier(
        // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
        @ServiceInternalApi dispatchers: MutableSet<NotifyDispatcher<*>>,
        context: Context
    ): Notifier {
      return Notifier.createDefault(context, dispatchers)
    }
  }
}
