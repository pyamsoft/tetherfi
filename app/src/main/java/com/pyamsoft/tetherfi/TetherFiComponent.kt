package com.pyamsoft.tetherfi

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.tetherfi.main.MainActivity
import com.pyamsoft.tetherfi.main.MainComponent
import com.pyamsoft.tetherfi.server.ServerModule
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.service.ProxyService
import com.pyamsoft.tetherfi.service.ServiceModule
import com.pyamsoft.tetherfi.service.ServicePreferences
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules =
        [
            TetherFiComponent.Provider::class,
            ServerModule::class,
            ServiceModule::class,
        ],
)
internal interface TetherFiComponent {

  @CheckResult fun plusMain(): MainComponent.Factory

  fun inject(service: ProxyService)

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
      internal fun provideActivityClass(): Class<out Activity> {
        return MainActivity::class.java
      }
    }
  }
}
