package com.pyamsoft.tetherfi

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.service.quicksettings.TileService
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.pydroid.notify.NotifyPermission
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.tetherfi.foreground.ForegroundServiceComponent
import com.pyamsoft.tetherfi.foreground.ProxyForegroundService
import com.pyamsoft.tetherfi.main.MainActivity
import com.pyamsoft.tetherfi.main.MainComponent
import com.pyamsoft.tetherfi.server.ServerAppModule
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.service.ServiceModule
import com.pyamsoft.tetherfi.service.ServicePreferences
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import com.pyamsoft.tetherfi.tile.ProxyTileComponent
import com.pyamsoft.tetherfi.tile.ProxyTileService
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules =
        [
            TetherFiComponent.Provider::class,
            ServerAppModule::class,
            ServiceModule::class,
        ],
)
internal interface TetherFiComponent {

  @CheckResult fun plusMain(): MainComponent.Factory

  @CheckResult fun plusForeground(): ForegroundServiceComponent.Factory

  @CheckResult fun plusTile(): ProxyTileComponent.Factory

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance application: Application,
        @Named("debug") @BindsInstance debug: Boolean,
        @BindsInstance imageLoader: ImageLoader,
        @BindsInstance theming: Theming,
    ): TetherFiComponent
  }

  @Module
  abstract class Provider {

    @Binds internal abstract fun bindServerPreferences(impl: PreferencesImpl): ServerPreferences

    @Binds internal abstract fun bindServicePreferences(impl: PreferencesImpl): ServicePreferences

    @Module
    companion object {

      @Provides
      @JvmStatic
      internal fun provideContext(application: Application): Context {
        return application
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
      internal fun provideActivityClass(): Class<out Activity> {
        return MainActivity::class.java
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
        return EventBus.create()
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun providePermissionResponseBus(): EventBus<PermissionResponse> {
        return EventBus.create()
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
