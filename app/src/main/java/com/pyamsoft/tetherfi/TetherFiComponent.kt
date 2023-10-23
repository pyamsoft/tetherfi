/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.service.quicksettings.TileService
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.bus.internal.DefaultEventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.pydroid.notify.NotifyPermission
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.foreground.ForegroundServiceComponent
import com.pyamsoft.tetherfi.foreground.ProxyForegroundService
import com.pyamsoft.tetherfi.main.MainActivity
import com.pyamsoft.tetherfi.main.MainComponent
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerAppModule
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WifiDirectAppModule
import com.pyamsoft.tetherfi.service.ServiceAppModule
import com.pyamsoft.tetherfi.service.ServicePreferences
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import com.pyamsoft.tetherfi.tile.ProxyTileActivity
import com.pyamsoft.tetherfi.tile.ProxyTileComponent
import com.pyamsoft.tetherfi.tile.ProxyTileService
import com.pyamsoft.tetherfi.tile.ProxyTileServiceComponent
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
@Component(
    modules =
        [
            TetherFiComponent.Provider::class,
            ServerAppModule::class,
            ServiceAppModule::class,
            WifiDirectAppModule::class,
        ],
)
internal interface TetherFiComponent {

  @CheckResult fun plusMain(): MainComponent.Factory

  @CheckResult fun plusForeground(): ForegroundServiceComponent.Factory

  @CheckResult fun plusTile(): ProxyTileComponent.Factory

  @CheckResult fun plusTileService(): ProxyTileServiceComponent.Factory

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
        @Named("debug") @BindsInstance debug: Boolean,
        @Named("app_scope") @BindsInstance scope: CoroutineScope,
        @BindsInstance application: Application,
        @BindsInstance imageLoader: ImageLoader,
        @BindsInstance theming: Theming,
        @BindsInstance enforcer: ThreadEnforcer,
    ): TetherFiComponent
  }

  @Module
  abstract class Provider {

    @Binds internal abstract fun bindConfigPreferences(impl: PreferencesImpl): ConfigPreferences

    @Binds internal abstract fun bindServerPreferences(impl: PreferencesImpl): ServerPreferences

    @Binds internal abstract fun bindServicePreferences(impl: PreferencesImpl): ServicePreferences

    @Binds
    internal abstract fun bindInAppRatingPreferences(impl: PreferencesImpl): InAppRatingPreferences

    @Binds
    internal abstract fun bindPermissionRequestConsumer(
        impl: EventBus<PermissionRequests>
    ): EventConsumer<PermissionRequests>

    @Binds
    internal abstract fun bindPermissionResponseConsumer(
        impl: EventBus<PermissionResponse>
    ): EventConsumer<PermissionResponse>

    @Module
    companion object {

      @Provides
      @JvmStatic
      internal fun provideContext(application: Application): Context {
        return application
      }

      @Provides
      @JvmStatic
      @Named("tile_activity")
      internal fun provideProxyTileActivityClass(): Class<out Activity> {
        return ProxyTileActivity::class.java
      }

      @Provides
      @JvmStatic
      @Named("main_activity")
      internal fun provideMainActivityClass(): Class<out Activity> {
        return MainActivity::class.java
      }

      @Provides
      @JvmStatic
      @Named("app_name")
      internal fun provideAppNameRes(): Int {
        return R.string.app_name
      }

      @Provides
      @JvmStatic
      @Singleton
      @Named("notification")
      internal fun provideNotificationPermissionRequester(): PermissionRequester {
        return NotifyPermission.createDefault()
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun provideNotifyGuard(context: Context): NotifyGuard {
        return NotifyGuard.createDefault(context)
      }

      @Provides
      @JvmStatic
      internal fun provideForegroundServiceClass(): Class<out Service> {
        return ProxyForegroundService::class.java
      }

      @Provides
      @JvmStatic
      internal fun provideTileServiceClass(): Class<out TileService> {
        return ProxyTileService::class.java
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun providePermissionRequestBus(): EventBus<PermissionRequests> {
        return DefaultEventBus()
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun providePermissionResponseBus(): EventBus<PermissionResponse> {
        return DefaultEventBus()
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun provideClock(): Clock {
        return Clock.systemDefaultZone()
      }
    }
  }
}
