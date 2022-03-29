package com.pyamsoft.widefi

import android.app.Application
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.util.isDebugMode

private const val PRIVACY_POLICY_URL = ""
private const val TERMS_CONDITIONS_URL = ""

class Widefi : Application() {

  private val component by lazy {
    val url = "https://github.com/pyamsoft/widefi"

    // Wrap in a lazy to ensure we only ever construct one
    val singletonLazyImageLoader by lazy(LazyThreadSafetyMode.NONE) { ImageLoader(this) }
    val lazyImageLoader = { singletonLazyImageLoader }

    val parameters =
        PYDroid.Parameters(
            viewSourceUrl = url,
            bugReportUrl = "$url/issues",
            privacyPolicyUrl = PRIVACY_POLICY_URL,
            termsConditionsUrl = TERMS_CONDITIONS_URL,
            version = BuildConfig.VERSION_CODE,
            imageLoader = lazyImageLoader,
            logger = createLogger(),
            theme = { activity, themeProvider, content ->
              activity.WidefiTheme(
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
      lazyImageLoader: () -> ImageLoader
  ): WidefiComponent {
    return DaggerWidefiComponent.factory()
        .create(
            this,
            isDebugMode(),
            lazyImageLoader,
            provider.get().theming(),
        )
        .also { addLibraries() }
  }

  override fun onCreate() {
    super.onCreate()
  }

  override fun getSystemService(name: String): Any? {
    // Use component here in a weird way to guarantee the lazy is initialized.
    return component.run { PYDroid.getSystemService(name) } ?: fallbackGetSystemService(name)
  }

  @CheckResult
  private fun fallbackGetSystemService(name: String): Any? {
    return if (name == WidefiComponent::class.java.name) component
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
    }
  }
}
