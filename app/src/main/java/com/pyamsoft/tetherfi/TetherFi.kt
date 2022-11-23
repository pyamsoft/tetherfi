package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import coil.ImageLoader
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.core.TERMS_CONDITIONS_URL
import timber.log.Timber

class TetherFi : Application() {

  // Must be lazy since Coil calls getSystemService() internally,
  // leading to SO exception
  private val lazyImageLoader = lazy(LazyThreadSafetyMode.NONE) { ImageLoader(this) }

  // The order that the PYDroid instance and TickerComponent instance are created is very specific.
  //
  // Coil lazy loader must be first, then PYDroid, and then Component
  private var pydroid: PYDroid? = null
  private var component: TetherFiComponent? = null

  private fun installPYDroid() {
    if (pydroid == null) {
      val url = "https://github.com/pyamsoft/tetherfi"

      installLogger()

      pydroid =
          PYDroid.init(
              this,
              PYDroid.Parameters(
                  // Must be lazy since Coil calls getSystemService() internally,
                  // leading to SO exception
                  lazyImageLoader = lazyImageLoader,
                  viewSourceUrl = url,
                  bugReportUrl = "$url/issues",
                  privacyPolicyUrl = PRIVACY_POLICY_URL,
                  termsConditionsUrl = TERMS_CONDITIONS_URL,
                  version = BuildConfig.VERSION_CODE,
                  logger = createLogger(),
                  theme = TetherFiThemeProvider,
                  debug =
                      PYDroid.DebugParameters(
                          enabled = true,
                          upgradeAvailable = true,
                          ratingAvailable = false,
                      ),
              ),
          )
    } else {
      Timber.w("Cannot install PYDroid again")
    }
  }

  private fun installComponent() {
    if (component == null) {
      val p = pydroid.requireNotNull { "Must install PYDroid before installing TetherFiComponent" }
      component =
          DaggerTetherFiComponent.factory()
              .create(
                  application = this,
                  debug = isDebugMode(),
                  lazyImageLoader = lazyImageLoader,
                  theming = p.modules().theming(),
              )
    } else {
      Timber.w("Cannot install TetherFiComponent again")
    }
  }

  @CheckResult
  private fun componentGraph(): TetherFiComponent {
    return component.requireNotNull { "TetherFiComponent was not installed, something is wrong." }
  }

  @CheckResult
  private fun fallbackGetSystemService(name: String): Any? {
    return if (name == TetherFiComponent::class.java.name) componentGraph()
    else super.getSystemService(name)
  }

  override fun onCreate() {
    super.onCreate()
    installPYDroid()
    installComponent()

    addLibraries()
  }

  override fun getSystemService(name: String): Any? {
    return pydroid?.getSystemService(name) ?: fallbackGetSystemService(name)
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
