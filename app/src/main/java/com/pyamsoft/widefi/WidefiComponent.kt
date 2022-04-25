package com.pyamsoft.widefi

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.widefi.main.MainActivity
import com.pyamsoft.widefi.main.MainComponent
import com.pyamsoft.widefi.server.ServerModule
import com.pyamsoft.widefi.server.ServerPreferences
import com.pyamsoft.widefi.service.ProxyService
import com.pyamsoft.widefi.service.ServiceModule
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
            WidefiComponent.Provider::class,
            ServerModule::class,
            ServiceModule::class,
        ],
)
internal interface WidefiComponent {

  @CheckResult fun plusMain(): MainComponent.Factory

  fun inject(service: ProxyService)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance application: Application,
        @Named("debug") @BindsInstance debug: Boolean,
        @BindsInstance lazyImageLoader: () -> ImageLoader,
        @BindsInstance theming: Theming,
    ): WidefiComponent
  }

  @Module
  abstract class Provider {

    @Binds
    internal abstract fun bindPreferences(impl: PreferencesImpl): ServerPreferences

    @Module
    companion object {

      @Provides
      @JvmStatic
      internal fun provideContext(application: Application): Context {
        return application
      }

      @Provides
      @JvmStatic
      @Singleton
      internal fun provideCoilImageLoader(lazyImageLoader: () -> ImageLoader): ImageLoader {
        return lazyImageLoader()
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
