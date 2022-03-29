package com.pyamsoft.widefi

import android.app.Application
import android.content.Context
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.widefi.server.ServerModule
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
        ],
)
internal interface WidefiComponent {

  fun inject(mainActivity: MainActivity)

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
    }
  }
}
