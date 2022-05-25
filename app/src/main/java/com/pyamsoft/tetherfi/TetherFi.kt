package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.core.TERMS_CONDITIONS_URL

class TetherFi : Application() {

  // Lazy so we ensure only one creation
  private val component by
      lazy(LazyThreadSafetyMode.NONE) {
        val url = "https://github.com/pyamsoft/tetherfi"

        val lazyImageLoader = lazy(LazyThreadSafetyMode.NONE) { ImageLoader(this) }

        val parameters =
            PYDroid.Parameters(
                // ImageLoader needs to be lazy because Coil calls getSystemService() internally,
                // which would otherwise lead to SO exception
                lazyImageLoader = lazyImageLoader,
                viewSourceUrl = url,
                bugReportUrl = "$url/issues",
                privacyPolicyUrl = PRIVACY_POLICY_URL,
                termsConditionsUrl = TERMS_CONDITIONS_URL,
                version = BuildConfig.VERSION_CODE,
                logger = createLogger(),
                theme = { activity, themeProvider, content ->
                  activity.TetherFiTheme(
                      themeProvider = themeProvider,
                      content = content,
                  )
                },
            )

        return@lazy createComponent(PYDroid.init(this, parameters), lazyImageLoader)
      }

  @CheckResult
  private fun createComponent(
      provider: ModuleProvider,
      lazyImageLoader: Lazy<ImageLoader>,
  ): TetherFiComponent {
    return DaggerTetherFiComponent.factory()
        .create(
            application = this,
            debug = isDebugMode(),
            lazyImageLoader = lazyImageLoader,
            theming = provider.get().theming(),
        )
        .also { addLibraries() }
  }

  override fun getSystemService(name: String): Any? {
    // Use component here in a weird way to guarantee the lazy is initialized.
    return component.run { PYDroid.getSystemService(name) } ?: fallbackGetSystemService(name)
  }

  @CheckResult
  private fun fallbackGetSystemService(name: String): Any? {
    return if (name == TetherFiComponent::class.java.name) component
    else {
      provideModuleDependencies(name) ?: super.getSystemService(name)
    }
  }

  @CheckResult
  private fun provideModuleDependencies(name: String): Any? {
    return component.run {
      when (name) {
        else -> null
      }
    }
  }

  companion object {

    @JvmStatic
    private fun addLibraries() {
      // We are using pydroid-notify
      OssLibraries.usingNotify = true

      // We are using pydroid-autopsy
      OssLibraries.usingAutopsy = true

      OssLibraries.add(
          "Dagger",
          "https://github.com/google/dagger",
          "A fast dependency injector for Android and Java.",
      )

      OssLibraries.add(
          "Ktor",
          "https://github.com/ktorio/ktor",
          "Framework for quickly creating connected applications in Kotlin with minimal effort",
      )
    }
  }
}
